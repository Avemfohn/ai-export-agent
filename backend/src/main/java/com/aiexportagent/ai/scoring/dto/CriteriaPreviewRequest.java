package com.aiexportagent.ai.scoring.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Criteria to try out without saving them. Any null field falls back to the
 * tenant's currently-saved value, so the client can preview a single edited
 * field in isolation.
 */
public record CriteriaPreviewRequest(
        JsonNode buyerCriteria,
        JsonNode targetSectors,
        JsonNode targetRegions
) {
}
