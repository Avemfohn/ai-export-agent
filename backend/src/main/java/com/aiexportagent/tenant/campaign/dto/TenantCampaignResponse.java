package com.aiexportagent.tenant.campaign.dto;

import com.aiexportagent.tenant.campaign.CampaignStatus;
import com.aiexportagent.tenant.campaign.TenantCampaign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The JSONB snapshot columns are returned as real JSON, matching
 * {@code TenantSettingsResponse} — they used to be escaped strings, which was
 * only tolerable while nothing consumed them.
 *
 * <p>{@code buyerCriteriaSnapshot} is still read by nothing: scoring builds its
 * prompt purely from tenant settings. It's exposed for completeness but has no
 * editor, deliberately — a field the customer can set that silently does
 * nothing is worse than no field at all.
 */
public record TenantCampaignResponse(
        UUID id,
        String name,
        String description,
        CampaignStatus status,
        JsonNode buyerCriteriaSnapshot,
        JsonNode emailDraftTemplateSnapshot,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TenantCampaignResponse from(TenantCampaign campaign, ObjectMapper objectMapper) {
        return new TenantCampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getStatus(),
                readOrNull(campaign.getBuyerCriteriaSnapshot(), objectMapper),
                readOrNull(campaign.getEmailDraftTemplateSnapshot(), objectMapper),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }

    /** Degrade one unparseable field to null rather than 500-ing the whole page. */
    private static JsonNode readOrNull(String raw, ObjectMapper objectMapper) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
