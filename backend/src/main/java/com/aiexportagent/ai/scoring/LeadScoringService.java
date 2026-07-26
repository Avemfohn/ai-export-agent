package com.aiexportagent.ai.scoring;

import com.aiexportagent.ai.client.AiClient;
import com.aiexportagent.ai.client.AiScoringRequest;
import com.aiexportagent.ai.client.AiScoringResult;
import com.aiexportagent.ai.scoring.dto.LeadScoringSummaryResponse;
import com.aiexportagent.global.supplier.GlobalSupplier;
import com.aiexportagent.global.supplier.GlobalSupplierService;
import com.aiexportagent.tenant.account.TenantSettings;
import com.aiexportagent.tenant.account.TenantSettingsService;
import com.aiexportagent.tenant.lead.LeadStatus;
import com.aiexportagent.tenant.lead.TenantLeadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        int matched = 0;
        int autoApproved = 0;
        int rejected = 0;
        int failed = 0;

        for (GlobalSupplier supplier : candidates) {
            try {
                AiScoringResult result = aiClient.score(toRequest(settings, supplier));
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

    private static AiScoringRequest toRequest(TenantSettings settings, GlobalSupplier supplier) {
        String combinedCriteria = """
                {"buyerCriteria": %s, "targetSectors": %s, "targetRegions": %s}"""
                .formatted(settings.getBuyerCriteria(), settings.getTargetSectors(), settings.getTargetRegions());
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
