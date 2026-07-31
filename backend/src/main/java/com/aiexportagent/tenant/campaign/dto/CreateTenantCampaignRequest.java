package com.aiexportagent.tenant.campaign.dto;

import com.aiexportagent.tenant.campaign.CampaignStatus;

/**
 * Creates a campaign for the current tenant.
 *
 * <p>{@code tenantId} is deliberately absent, and absent means unwritable —
 * the service takes it from {@code TenantContext}, same rule as every other
 * write path here.
 *
 * <p>{@code status} accepts only {@code DRAFT} or {@code ACTIVE} (defaulting
 * to {@code DRAFT} when null). Anything else would mean creating a campaign
 * that is already paused/completed/archived, which has no meaning. The UI
 * presents this as an "activate immediately" choice rather than a status
 * picker, because a campaign created as DRAFT silently parks every lead
 * assigned to it.
 *
 * <p>There is no {@code emailDraftTemplate} field: creation copies the
 * tenant's current default template as the campaign's starting snapshot, which
 * is what "snapshot" means and what lets the editor open on something real.
 */
public record CreateTenantCampaignRequest(
        String name,
        String description,
        CampaignStatus status
) {
}
