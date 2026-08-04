package com.aiexportagent.scraping.upload;

import com.aiexportagent.common.exception.ApiException;
import com.aiexportagent.global.contact.GlobalSupplierContact;
import com.aiexportagent.global.contact.GlobalSupplierContactService;
import com.aiexportagent.global.supplier.GlobalSupplier;
import com.aiexportagent.global.supplier.GlobalSupplierService;
import com.aiexportagent.scraping.upload.dto.UploadSummaryResponse;
import com.aiexportagent.tenant.scraping.ScrapingJob;
import com.aiexportagent.tenant.scraping.ScrapingJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Turns an uploaded exhibitor list into shared-pool suppliers and contacts.
 *
 * <p>This is the first code in the product that writes to {@code
 * global_suppliers} — a table with <strong>no {@code tenant_id}</strong> that
 * every tenant's scoring reads. Three rules follow from that and none of them
 * are negotiable:
 *
 * <ol>
 *   <li><strong>Add only.</strong> An existing domain is skipped whole. One
 *       tenant's spreadsheet must never rewrite a company record another
 *       tenant depends on. This also caps how much damage a malicious file can
 *       do: only domains nobody has pooled yet.</li>
 *   <li><strong>No tenant_id ever reaches a pool row.</strong> The only
 *       tenant-scoped write is the {@code scraping_jobs} row, and its tenant
 *       comes from {@code TenantContext}, never from the request.</li>
 *   <li><strong>Contacts only for suppliers this import created</strong>, and
 *       only when the address survives the trust rules below.</li>
 * </ol>
 *
 * <p><strong>All rejection happens before any transaction opens.</strong> With
 * JDBC batching on, inserts queue until flush, so a constraint violation
 * surfaces at commit, poisons the persistence context and takes the entire
 * chunk with it — a per-row try/catch around {@code save()} would catch nothing
 * and quietly lose 500 good rows. Validating up front is what makes "import the
 * 397 good rows out of 400" actually true.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierUploadService {

    private final GlobalSupplierService globalSupplierService;
    private final GlobalSupplierContactService globalSupplierContactService;
    private final ScrapingJobService scrapingJobService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.enabled:false}")
    private boolean uploadEnabled;

    @Value("${app.upload.max-rows:5000}")
    private int maxRows;

    @Value("${app.upload.chunk-size:500}")
    private int chunkSize;

    /**
     * Providers where the email domain says nothing about which company the
     * person works for. A huge share of real SMB exhibitors publish one of
     * these, so rejecting them outright would leave those suppliers with no
     * contact and therefore permanently unemailable.
     */
    private static final Set<String> FREEMAIL_DOMAINS = Set.of(
            "gmail.com", "googlemail.com", "hotmail.com", "hotmail.co.uk", "outlook.com",
            "live.com", "msn.com", "yahoo.com", "yahoo.co.uk", "ymail.com", "icloud.com",
            "me.com", "aol.com", "gmx.com", "gmx.de", "mail.ru", "yandex.com", "yandex.ru",
            "protonmail.com", "proton.me", "zoho.com", "mynet.com", "superonline.com");

    private static final BigDecimal CONFIDENCE_DOMAIN_MATCH = new BigDecimal("0.800");
    private static final BigDecimal CONFIDENCE_FREEMAIL = new BigDecimal("0.300");

    /**
     * Parse and report what would happen. Reads the pool to work out how much
     * is already there, but <strong>writes nothing</strong>.
     *
     * <p>The already-pooled count is the whole point of this screen: uploading
     * 500 rows and seeing 40 created reads as data loss unless the other 460
     * are explained as "another tenant already contributed these".
     */
    public UploadSummaryResponse preview(String fileName, byte[] bytes) {
        requireEnabled();
        ParsedUpload parsed = parseAndValidate(fileName, bytes);
        return parsed.toSummary(null, fileName, sha256(bytes), countAlreadyPooled(parsed.candidates));
    }

    /** Chunked so a 5,000-row file does not build one enormous IN clause. */
    private int countAlreadyPooled(List<Candidate> candidates) {
        int pooled = 0;
        for (int start = 0; start < candidates.size(); start += chunkSize) {
            List<Candidate> chunk = candidates.subList(
                    start, Math.min(start + chunkSize, candidates.size()));
            Set<String> existing = globalSupplierService.findExistingDomains(
                    chunk.stream().map(Candidate::domain).toList());
            pooled += (int) chunk.stream().filter(c -> existing.contains(c.domain())).count();
        }
        return pooled;
    }

    /**
     * Parse and commit.
     *
     * <p>Re-runs every parse, validation and sanitisation step from scratch —
     * preview is a UX affordance, not a validation pass, and this path must
     * stand alone.
     */
    public UploadSummaryResponse commit(String fileName, byte[] bytes) {
        requireEnabled();
        ParsedUpload parsed = parseAndValidate(fileName, bytes);
        String sha = sha256(bytes);

        // Committed before any supplier insert: pool rows carry
        // source_scraping_job_id, so the job must already exist and be visible.
        ScrapingJob job = scrapingJobService.startUploadJob(jobParams(fileName, sha, parsed.rowsRead));

        int created = 0;
        int contactsCreated = 0;
        try {
            List<Candidate> candidates = parsed.candidates;
            for (int start = 0; start < candidates.size(); start += chunkSize) {
                List<Candidate> chunk = candidates.subList(
                        start, Math.min(start + chunkSize, candidates.size()));
                ChunkResult result = persistChunk(chunk, job.getId());
                created += result.created();
                contactsCreated += result.contactsCreated();
            }
        } catch (RuntimeException e) {
            log.error("Trade-fair upload {} failed after {} suppliers", job.getId(), created, e);
            scrapingJobService.finishUploadJob(job.getId(), "FAILED",
                    parsed.summaryJson(objectMapper, created, contactsCreated),
                    created, e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The import failed partway through. " + created
                            + " companies were added; re-uploading the same file will import the rest.");
        }

        scrapingJobService.finishUploadJob(job.getId(), "COMPLETED",
                parsed.summaryJson(objectMapper, created, contactsCreated), created, null);

        return parsed.toSummary(job.getId(), fileName, sha, 0)
                .withCounts(created, contactsCreated);
    }

    private void requireEnabled() {
        if (!uploadEnabled) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "File upload is disabled on this deployment.");
        }
    }

    /**
     * One chunk, one transaction. The dedup read happens inside it so a
     * concurrent import of the same domain is caught by the unique constraint
     * rather than silently double-inserting.
     */
    private ChunkResult persistChunk(List<Candidate> chunk, UUID jobId) {
        Set<String> existing = globalSupplierService.findExistingDomains(
                chunk.stream().map(Candidate::domain).toList());

        List<GlobalSupplier> toCreate = new ArrayList<>();
        List<Candidate> creating = new ArrayList<>();
        for (Candidate candidate : chunk) {
            if (existing.contains(candidate.domain())) continue;
            toCreate.add(candidate.toSupplier(jobId));
            creating.add(candidate);
        }
        if (toCreate.isEmpty()) return new ChunkResult(0, 0);

        List<GlobalSupplier> saved = globalSupplierService.createAll(toCreate);

        List<GlobalSupplierContact> contacts = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            Candidate candidate = creating.get(i);
            if (candidate.contact() == null) continue;
            contacts.add(candidate.contact().toEntity(saved.get(i).getId()));
        }
        if (!contacts.isEmpty()) {
            globalSupplierContactService.createAll(contacts);
        }
        return new ChunkResult(saved.size(), contacts.size());
    }

    // ---------------------------------------------------------------- parsing

    private ParsedUpload parseAndValidate(String fileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The uploaded file is empty.");
        }

        SpreadsheetParser.Sheet2D sheet = SpreadsheetParser.parse(fileName, bytes, maxRows);
        ColumnDetector.Detection detection = ColumnDetector.detect(sheet.headers());

        List<UploadColumn> missing = detection.missingRequired();
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Could not find a column for: " + missing.stream().map(Enum::name).toList()
                            + ". The file needs a company name column and a website column.");
        }

        ParsedUpload parsed = new ParsedUpload(detection);
        Set<String> seenDomains = new HashSet<>();
        Set<String> seenSupplierEmails = new HashSet<>();

        for (List<String> row : sheet.rows()) {
            parsed.rowsRead++;

            String name = CellSanitizer.clean(cell(row, detection, UploadColumn.COMPANY_NAME), 500);
            if (name == null) {
                parsed.skippedNoName++;
                continue;
            }

            Optional<String> domain = DomainNormalizer.normalize(
                    cell(row, detection, UploadColumn.WEBSITE));
            if (domain.isEmpty()) {
                parsed.skippedNoDomain++;
                continue;
            }
            // Deduping in-file matters as much as against the database: the
            // same domain twice would otherwise hit the unique constraint at
            // flush and take the whole chunk down.
            if (!seenDomains.add(domain.get())) {
                parsed.duplicateInFile++;
                continue;
            }

            String country = CellSanitizer.clean(cell(row, detection, UploadColumn.COUNTRY), 100);
            String city = CellSanitizer.clean(cell(row, detection, UploadColumn.CITY), 255);
            String sector = CellSanitizer.clean(cell(row, detection, UploadColumn.SECTOR), 255);
            String description = describe(
                    CellSanitizer.clean(cell(row, detection, UploadColumn.DESCRIPTION), 4000),
                    name, sector, city, country);

            ContactCandidate contact = buildContact(row, detection, domain.get(),
                    seenSupplierEmails, parsed);

            parsed.candidates.add(new Candidate(
                    name, domain.get(), country, city, sector, description, contact));
        }

        return parsed;
    }

    /**
     * Real exhibitor lists carry no description column, and a supplier with no
     * description scores 20 against a threshold of 60 — every uploaded company
     * would be rejected before anyone saw it. Compose one from the row's own
     * facts so scoring has something real to work with.
     *
     * <p>Strictly derived from the file: nothing is invented about the company.
     */
    private static String describe(String provided, String name, String sector,
                                   String city, String country) {
        if (provided != null) return provided;

        StringBuilder sb = new StringBuilder(name);
        if (sector != null) sb.append(" — ").append(sector);
        if (city != null && country != null) {
            sb.append(", based in ").append(city).append(", ").append(country);
        } else if (country != null) {
            sb.append(", based in ").append(country);
        }
        sb.append(". Listed as a trade-fair exhibitor.");
        return sb.toString();
    }

    /**
     * Apply the contact trust rules.
     *
     * <p>The mismatched-custom-domain case is the anti-squatting control. A
     * malicious upload of {@code competitor.com} carrying
     * {@code attacker@evil-corp.com} would otherwise have another tenant's
     * scoring pick up the company and the automated pipeline deliver that
     * tenant's pitch and pricing straight to the attacker — with no bug
     * anywhere, every component behaving as designed.
     *
     * <p>Freemail cannot be treated the same way without discarding a large
     * share of legitimate SMB contacts, so it is imported at low confidence and
     * gated at the send step instead (see {@code OutreachDraftingService}).
     */
    private ContactCandidate buildContact(List<String> row, ColumnDetector.Detection detection,
                                          String supplierDomain, Set<String> seenSupplierEmails,
                                          ParsedUpload parsed) {
        String email = CellSanitizer.clean(cell(row, detection, UploadColumn.CONTACT_EMAIL), 320);
        if (email == null) return null;

        email = email.toLowerCase(Locale.ROOT);
        Optional<String> emailDomain = DomainNormalizer.domainOfEmail(email);
        if (emailDomain.isEmpty()) return null;

        // The partial unique index on (global_supplier_id, email) would kill the
        // chunk at flush; a sheet listing two people with one shared address is
        // ordinary, not hostile.
        if (!seenSupplierEmails.add(supplierDomain + "|" + email)) return null;

        BigDecimal confidence;
        if (emailDomain.get().equals(supplierDomain)) {
            confidence = CONFIDENCE_DOMAIN_MATCH;
        } else if (FREEMAIL_DOMAINS.contains(emailDomain.get())) {
            confidence = CONFIDENCE_FREEMAIL;
            parsed.contactsLowConfidence++;
        } else {
            parsed.contactsDroppedForeignDomain++;
            return null;
        }

        return new ContactCandidate(
                CellSanitizer.clean(cell(row, detection, UploadColumn.CONTACT_NAME), 255),
                CellSanitizer.clean(cell(row, detection, UploadColumn.CONTACT_TITLE), 255),
                email,
                CellSanitizer.clean(cell(row, detection, UploadColumn.CONTACT_PHONE), 50),
                confidence);
    }

    private static String cell(List<String> row, ColumnDetector.Detection detection,
                               UploadColumn column) {
        Integer index = detection.indexOf(column);
        if (index == null || index >= row.size()) return null;
        return row.get(index);
    }

    private String jobParams(String fileName, String sha, int rowsRead) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("fileName", fileName);
        node.put("sha256", sha);
        node.put("rowsRead", rowsRead);
        return node.toString();
    }

    static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    // ------------------------------------------------------------- value types

    private record ChunkResult(int created, int contactsCreated) {
    }

    private record ContactCandidate(String fullName, String jobTitle, String email,
                                    String phone, BigDecimal confidence) {
        GlobalSupplierContact toEntity(UUID supplierId) {
            GlobalSupplierContact contact = new GlobalSupplierContact();
            contact.setGlobalSupplierId(supplierId);
            contact.setFullName(fullName);
            contact.setJobTitle(jobTitle);
            contact.setEmail(email);
            contact.setPhone(phone);
            contact.setPrimary(true);
            contact.setConfidenceScore(confidence);
            contact.setSource("TRADE_FAIR_UPLOAD");
            return contact;
        }
    }

    private record Candidate(String companyName, String domain, String country, String city,
                             String sector, String description, ContactCandidate contact) {
        GlobalSupplier toSupplier(UUID jobId) {
            GlobalSupplier supplier = new GlobalSupplier();
            supplier.setCompanyName(companyName);
            supplier.setDomain(domain);
            supplier.setWebsiteUrl("https://" + domain);
            supplier.setCountry(country);
            supplier.setCity(city);
            supplier.setSector(sector);
            supplier.setDescription(description);
            supplier.setSource("TRADE_FAIR_UPLOAD");
            supplier.setEnrichmentData("{}");
            supplier.setLastScrapedAt(OffsetDateTime.now());
            // Provenance, not ownership: points at a tenant-scoped job so the
            // contributing tenant stays traceable without a tenant_id here.
            supplier.setSourceScrapingJobId(jobId);
            return supplier;
        }
    }

    /** Mutable accumulator used only while parsing a single upload. */
    private static final class ParsedUpload {
        private final ColumnDetector.Detection detection;
        private final List<Candidate> candidates = new ArrayList<>();
        private int rowsRead;
        private int skippedNoDomain;
        private int skippedNoName;
        private int duplicateInFile;
        private int contactsDroppedForeignDomain;
        private int contactsLowConfidence;

        ParsedUpload(ColumnDetector.Detection detection) {
            this.detection = detection;
        }

        String summaryJson(ObjectMapper mapper, int created, int contactsCreated) {
            ObjectNode node = mapper.createObjectNode();
            node.put("rowsRead", rowsRead);
            node.put("created", created);
            node.put("contactsCreated", contactsCreated);
            node.put("skippedNoDomain", skippedNoDomain);
            node.put("skippedNoName", skippedNoName);
            node.put("duplicateInFile", duplicateInFile);
            node.put("contactsDroppedForeignDomain", contactsDroppedForeignDomain);
            node.put("contactsLowConfidence", contactsLowConfidence);
            return node.toString();
        }

        UploadSummaryResponse toSummary(UUID jobId, String fileName, String sha, int alreadyPooled) {
            Map<String, String> detected = new LinkedHashMap<>();
            detection.indexByColumn().forEach((column, index) ->
                    detected.put(column.name(), "column " + (index + 1)));

            List<UploadSummaryResponse.SampleRow> samples = candidates.stream()
                    .limit(5)
                    .map(c -> new UploadSummaryResponse.SampleRow(
                            c.companyName(), c.domain(), c.country(), c.sector(),
                            c.contact() == null ? null : c.contact().email()))
                    .toList();

            return new UploadSummaryResponse(
                    jobId, fileName, sha,
                    detected.entrySet().stream()
                            .map(e -> new UploadSummaryResponse.DetectedColumn(e.getKey(), e.getValue()))
                            .toList(),
                    detection.unmatchedHeaders(),
                    rowsRead,
                    candidates.size() - alreadyPooled,
                    alreadyPooled,
                    skippedNoDomain,
                    skippedNoName,
                    duplicateInFile,
                    (int) candidates.stream().filter(c -> c.contact() != null).count(),
                    contactsDroppedForeignDomain,
                    contactsLowConfidence,
                    samples);
        }
    }
}
