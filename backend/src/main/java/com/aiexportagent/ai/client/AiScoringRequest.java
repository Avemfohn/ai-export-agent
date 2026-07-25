package com.aiexportagent.ai.client;

/**
 * Everything an {@link AiClient} needs to score one global_suppliers row
 * against one tenant's buyer criteria. {@code buyerCriteriaJson} combines
 * tenant_settings.buyer_criteria, target_sectors, and target_regions —
 * three separate JSONB columns, but conceptually one "buyer criteria"
 * concern — as raw JSON text, intentionally not parsed into a POJO (see
 * CLAUDE.md conventions) so the LLM interprets it holistically rather than
 * the app imposing a rigid criteria schema. See
 * {@code LeadScoringService#toRequest}.
 */
public record AiScoringRequest(
        String buyerCriteriaJson,
        String companyName,
        String domain,
        String country,
        String sector,
        String description
) {
}
