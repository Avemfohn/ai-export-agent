package com.aiexportagent.scraping.upload.dto;

import java.util.List;
import java.util.UUID;

/**
 * What an upload did, or would do. Returned by both the preview and the commit
 * endpoint so the confirm screen and the result screen render from one shape.
 *
 * <p>Every rejection is <strong>counted, not silently dropped</strong>. A file
 * that produces "0 suppliers created" with no explanation is indistinguishable
 * from a broken parser, and the shared pool makes that worse: a customer who
 * uploads 500 rows and sees 40 created needs to be told the other 460 were
 * already pooled, or it reads as data loss.
 *
 * @param scrapingJobId   the recorded job, null for a preview (which writes nothing)
 * @param fileName        as uploaded, echoed back for confirmation
 * @param sha256          integrity check only — see SupplierUploadController
 * @param detectedColumns which field each spreadsheet column was matched to
 * @param unmatchedHeaders headers that matched nothing, so a missed mapping is visible
 * @param rowsRead        data rows the parser saw, excluding the header
 * @param created         suppliers newly added to the shared pool
 * @param alreadyPooled   domains another upload or scrape had already contributed
 * @param skippedNoDomain rows with no usable website
 * @param skippedNoName   rows with no usable company name
 * @param duplicateInFile the same domain appearing twice in this one file
 * @param contactsCreated contacts attached to newly created suppliers
 * @param contactsDroppedForeignDomain contacts on an unrelated custom domain — the
 *                        anti-squatting control, counted so it is never invisible
 * @param contactsLowConfidence freemail contacts stored but barred from automated sending
 * @param sampleRows      a few parsed rows so the user can eyeball the mapping
 */
public record UploadSummaryResponse(
        UUID scrapingJobId,
        String fileName,
        String sha256,
        List<DetectedColumn> detectedColumns,
        List<String> unmatchedHeaders,
        int rowsRead,
        int created,
        int alreadyPooled,
        int skippedNoDomain,
        int skippedNoName,
        int duplicateInFile,
        int contactsCreated,
        int contactsDroppedForeignDomain,
        int contactsLowConfidence,
        List<SampleRow> sampleRows
) {

    /**
     * Replace the projected counts with what the commit actually did.
     *
     * <p>A preview reports what <em>would</em> happen; after committing, the
     * real numbers can differ — a concurrent import may have pooled one of the
     * same domains in between.
     */
    public UploadSummaryResponse withCounts(int actualCreated, int actualContactsCreated) {
        return new UploadSummaryResponse(
                scrapingJobId, fileName, sha256, detectedColumns, unmatchedHeaders, rowsRead,
                actualCreated,
                alreadyPooled + (created - actualCreated),
                skippedNoDomain, skippedNoName, duplicateInFile,
                actualContactsCreated, contactsDroppedForeignDomain, contactsLowConfidence,
                sampleRows);
    }

    /** @param field the UploadColumn name; @param header the sheet header it matched */
    public record DetectedColumn(String field, String header) {
    }

    public record SampleRow(String companyName, String domain, String country,
                            String sector, String contactEmail) {
    }
}
