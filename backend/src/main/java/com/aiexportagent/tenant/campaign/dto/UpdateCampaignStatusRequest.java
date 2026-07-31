package com.aiexportagent.tenant.campaign.dto;

import com.aiexportagent.tenant.campaign.CampaignStatus;

/** Narrow status gate — transitions are validated by {@link CampaignStatus#canTransitionTo}. */
public record UpdateCampaignStatusRequest(CampaignStatus status) {
}
