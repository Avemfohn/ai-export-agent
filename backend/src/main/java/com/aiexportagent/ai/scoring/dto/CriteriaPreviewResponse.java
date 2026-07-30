package com.aiexportagent.ai.scoring.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of trying criteria against a small sample of the supplier pool.
 * Nothing is persisted — {@code wouldApprove} describes what scoring
 * <em>would</em> do, not what it did.
 */
public record CriteriaPreviewResponse(
        int sampleSize,
        List<ScoredSample> samples
) {

    public record ScoredSample(
            String companyName,
            String domain,
            String country,
            String sector,
            BigDecimal score,
            String rationale,
            boolean wouldApprove,
            boolean wouldReject
    ) {
    }
}
