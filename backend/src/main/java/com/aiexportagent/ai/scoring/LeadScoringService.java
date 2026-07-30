package com.aiexportagent.ai.scoring;

import com.aiexportagent.ai.client.AiClient;
import com.aiexportagent.ai.client.AiScoringRequest;
import com.aiexportagent.ai.client.AiScoringResult;
import com.aiexportagent.ai.scoring.dto.CriteriaPreviewRequest;
import com.aiexportagent.ai.scoring.dto.CriteriaPreviewResponse;
import com.aiexportagent.ai.scoring.dto.LeadScoringSummaryResponse;
import com.aiexportagent.common.exception.ApiException;
import com.aiexportagent.common.validation.SettingsJsonValidator;
import com.aiexportagent.global.supplier.GlobalSupplier;
import com.aiexportagent.global.supplier.GlobalSupplierService;
import com.aiexportagent.tenant.account.TenantSettings;
import com.aiexportagent.tenant.account.TenantSettingsService;
import com.aiexportagent.tenant.lead.LeadStatus;
import com.aiexportagent.tenant.lead.TenantLeadService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates AI lead scoring for the current tenant: evaluates every
 * global_suppliers row this tenant doesn't already have a lead for against
 * the tenant's buyer_criteria, and creates a scored tenant_lead per
 * candidate. Create-only / idempotent — never touches an existing lead, so
 * it's safe to re-run and never disturbs curated/manually-managed leads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadScoringService {

    /** Capped because a real provider bills one call per supplier here. */
    private static final int PREVIEW_SAMPLE_SIZE = 5;

    private final TenantSettingsService tenantSettingsService;
    private final GlobalSupplierService globalSupplierService;
    private final TenantLeadService tenantLeadService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.match-threshold:60}")
    private int matchThreshold;

    /**
     * Deliberately NOT @Transactional at this level — the loop below makes a
     * synchronous external AI HTTP call per candidate, and holding one DB
     * transaction open across all of them would starve the connection pool
     * once real providers are wired in. Each read call below has its own
     * short transaction (see the called services), and
     * {@link TenantLeadService#createAiScoredLead} opens its own
     * REQUIRES_NEW transaction per lead, committing immediately.
     *
     * <p>Status decision is 3-way, checked in this order: (1) if the
     * tenant's {@code auto_approve_threshold} is set and the score meets or
     * exceeds it, the lead is created directly as {@code APPROVED} —
     * skipping manual review entirely; (2) else if the score meets the
     * global {@code app.ai.match-threshold}, {@code PENDING_APPROVAL}; (3)
     * else {@code REJECTED}. A tenant that sets an auto-approve threshold
     * below the match threshold will see branch (1) win for scores that
     * would otherwise have only reached {@code PENDING_APPROVAL} — this is
     * allowed and not cross-validated, since enforcing an ordering would
     * require wiring {@code matchThreshold} into {@code TenantSettingsService}
     * for what is a soft misconfiguration, not a correctness issue.
     */
    public LeadScoringSummaryResponse scoreForCurrentTenant() {
        TenantSettings settings = tenantSettingsService.getForCurrentTenant();
        Set<UUID> alreadyLinked = tenantLeadService.getLinkedGlobalSupplierIdsForCurrentTenant();

        List<GlobalSupplier> candidates = globalSupplierService.findAll().stream()
                .filter(supplier -> !alreadyLinked.contains(supplier.getId()))
                .toList();

        BigDecimal autoApproveThreshold = settings.getAutoApproveThreshold();
        String combinedCriteria = buildCriteriaEnvelope(settings).toString();
        int matched = 0;
        int autoApproved = 0;
        int rejected = 0;
        int failed = 0;

        for (GlobalSupplier supplier : candidates) {
            try {
                AiScoringResult result = aiClient.score(toRequest(combinedCriteria, supplier));
                BigDecimal score = BigDecimal.valueOf(result.score());

                LeadStatus status;
                if (autoApproveThreshold != null && score.compareTo(autoApproveThreshold) >= 0) {
                    status = LeadStatus.APPROVED;
                } else if (result.score() >= matchThreshold) {
                    status = LeadStatus.PENDING_APPROVAL;
                } else {
                    status = LeadStatus.REJECTED;
                }

                tenantLeadService.createAiScoredLead(
                        supplier.getId(),
                        status,
                        score,
                        result.rationale(),
                        toMetadataJson(result));

                switch (status) {
                    case APPROVED -> autoApproved++;
                    case PENDING_APPROVAL -> matched++;
                    default -> rejected++;
                }
            } catch (Exception e) {
                // Catches both AI-call failures (AiClientException) and DB-write failures
                // (e.g. a unique-constraint race from a concurrent trigger for this tenant) —
                // one bad supplier shouldn't abort leads already committed for the others,
                // now that each createAiScoredLead() call commits independently.
                failed++;
                log.warn("AI scoring failed for supplier {} ({}): {}",
                        supplier.getId(), supplier.getDomain(), e.getMessage());
            }
        }

        return new LeadScoringSummaryResponse(candidates.size(), matched, autoApproved, rejected, failed);
    }

    /**
     * Scores a small sample of the supplier pool against candidate criteria
     * <strong>without writing anything</strong> — no tenant_leads, no settings
     * update. Exists because scoring only ever considers suppliers the tenant
     * has no lead for, so once a tenant has been scored, editing criteria and
     * re-running produces no observable change; this is how a customer sees
     * what their edit actually does.
     *
     * <p>Criteria come from the request where provided (so unsaved edits can be
     * tried) and fall back to the saved values otherwise. Validated with the
     * same rules as a real save, so preview can't be used to smuggle a shape
     * the write path would reject.
     *
     * <p>The sample is capped: with a real provider this is one billed call per
     * supplier, and the point is a sanity check, not a full re-score.
     *
     * <p>Unlike {@link #scoreForCurrentTenant()}, the sample deliberately does
     * <em>not</em> exclude suppliers the tenant already has a lead for. That
     * exclusion is exactly why editing criteria otherwise appears to do nothing
     * — once a tenant has been scored, every supplier is linked and a re-run
     * evaluates zero candidates. Filtering here would reproduce that dead end
     * and leave the preview permanently empty.
     */
    public CriteriaPreviewResponse previewCriteriaForCurrentTenant(CriteriaPreviewRequest request) {
        TenantSettings settings = tenantSettingsService.getForCurrentTenant();

        JsonNode buyerCriteria = request.buyerCriteria();
        JsonNode targetSectors = request.targetSectors();
        JsonNode targetRegions = request.targetRegions();

        if (buyerCriteria != null) {
            SettingsJsonValidator.validateBuyerCriteria(buyerCriteria, "Buyer criteria");
        }
        if (targetSectors != null) {
            SettingsJsonValidator.validateStringArray(targetSectors, "Target sectors");
        }
        if (targetRegions != null) {
            SettingsJsonValidator.validateStringArray(targetRegions, "Target regions");
        }

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.set("buyerCriteria",
                buyerCriteria != null ? buyerCriteria : readJson(settings.getBuyerCriteria(), "buyer criteria"));
        envelope.set("targetSectors",
                targetSectors != null ? targetSectors : readJson(settings.getTargetSectors(), "target sectors"));
        envelope.set("targetRegions",
                targetRegions != null ? targetRegions : readJson(settings.getTargetRegions(), "target regions"));
        String combinedCriteria = envelope.toString();

        BigDecimal autoApproveThreshold = settings.getAutoApproveThreshold();
        List<GlobalSupplier> sample = globalSupplierService.findAll().stream()
                .limit(PREVIEW_SAMPLE_SIZE)
                .toList();

        List<CriteriaPreviewResponse.ScoredSample> scored = new ArrayList<>();
        for (GlobalSupplier supplier : sample) {
            try {
                AiScoringResult result = aiClient.score(toRequest(combinedCriteria, supplier));
                BigDecimal score = BigDecimal.valueOf(result.score());
                boolean wouldApprove =
                        autoApproveThreshold != null && score.compareTo(autoApproveThreshold) >= 0;
                boolean wouldReject = result.score() < matchThreshold;
                scored.add(new CriteriaPreviewResponse.ScoredSample(
                        supplier.getCompanyName(),
                        supplier.getDomain(),
                        supplier.getCountry(),
                        supplier.getSector(),
                        score,
                        result.rationale(),
                        wouldApprove,
                        wouldReject));
            } catch (Exception e) {
                // Preview is best-effort: one unscoreable supplier shouldn't blank the whole panel.
                log.warn("Criteria preview failed for supplier {}: {}", supplier.getId(), e.getMessage());
            }
        }

        return new CriteriaPreviewResponse(scored.size(), scored);
    }

    /**
     * Builds the combined-criteria envelope through Jackson rather than by
     * string-splicing the three JSONB columns into a hand-written template.
     *
     * <p>The splice was not actually broken — {@code jsonb} guarantees each
     * column holds syntactically valid JSON and all three are NOT NULL, so
     * concatenation always produced a valid envelope. This is a robustness
     * change, not a bug fix: it removes a construction where the validity of
     * the prompt depended on an invariant enforced two layers away, and it
     * would have been the first thing to break if any of these columns were
     * ever made nullable or moved off {@code jsonb}.
     *
     * <p>Built once before the loop rather than per supplier, so the
     * unparseable case (only reachable if the column type changes) fails the
     * run once with a message naming the bad field instead of burning one AI
     * call per supplier.
     */
    private ObjectNode buildCriteriaEnvelope(TenantSettings settings) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.set("buyerCriteria", readJson(settings.getBuyerCriteria(), "buyer criteria"));
        envelope.set("targetSectors", readJson(settings.getTargetSectors(), "target sectors"));
        envelope.set("targetRegions", readJson(settings.getTargetRegions(), "target regions"));
        return envelope;
    }

    private JsonNode readJson(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Your saved " + label + " is not valid JSON. Re-save it in Settings to fix this.");
        }
    }

    private AiScoringRequest toRequest(String combinedCriteria, GlobalSupplier supplier) {
        return new AiScoringRequest(
                combinedCriteria,
                supplier.getCompanyName(),
                supplier.getDomain(),
                supplier.getCountry(),
                supplier.getSector(),
                supplier.getDescription());
    }

    private String toMetadataJson(AiScoringResult result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", result.provider());
        metadata.put("model", result.model());
        metadata.put("rawScore", result.score());
        metadata.put("scoredAt", OffsetDateTime.now().toString());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            // Should not happen for a plain string/int map — fall back to an empty object
            // rather than fail the whole scoring call over a metadata-serialization glitch.
            log.warn("Failed to serialize ai_match_metadata, storing empty object", e);
            return "{}";
        }
    }
}
