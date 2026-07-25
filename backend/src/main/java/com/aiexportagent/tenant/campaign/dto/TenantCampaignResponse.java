package com.aiexportagent.tenant.campaign.dto;

import com.aiexportagent.tenant.campaign.TenantCampaign;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantCampaignResponse(
        UUID id,
        String name,
        String description,
        String status,
        String buyerCriteriaSnapshot,
        String emailDraftTemplateSnapshot,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TenantCampaignResponse from(TenantCampaign campaign) {
        return new TenantCampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getStatus(),
                campaign.getBuyerCriteriaSnapshot(),
                campaign.getEmailDraftTemplateSnapshot(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
