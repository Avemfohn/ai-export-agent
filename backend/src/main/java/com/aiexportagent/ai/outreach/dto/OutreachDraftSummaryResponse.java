package com.aiexportagent.ai.outreach.dto;

public record OutreachDraftSummaryResponse(
        int leadsEvaluated,
        int drafted,
        int skippedNoContact,
        int failed
) {
}
