package com.aiexportagent.ai.scoring.dto;

public record LeadScoringSummaryResponse(
        int suppliersEvaluated,
        int matched,
        int autoApproved,
        int rejected,
        int failed
) {
}
