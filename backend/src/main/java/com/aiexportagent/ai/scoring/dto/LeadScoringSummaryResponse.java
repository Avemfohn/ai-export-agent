package com.aiexportagent.ai.scoring.dto;

public record LeadScoringSummaryResponse(
        int suppliersEvaluated,
        int matched,
        int rejected,
        int failed
) {
}
