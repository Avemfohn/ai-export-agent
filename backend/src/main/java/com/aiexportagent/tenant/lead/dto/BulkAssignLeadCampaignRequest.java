package com.aiexportagent.tenant.lead.dto;

import java.util.List;
import java.util.UUID;

/**
 * Assigns leads to a campaign, or removes them from one.
 *
 * <p><strong>{@code tenantCampaignId == null} means "unassign", not "leave
 * unchanged".</strong> That reads like a violation of the null-means-unchanged
 * convention {@code UpdateTenantSettingsRequest} documents, but it isn't: this
 * endpoint writes exactly one field and always writes it, so there is no
 * "unchanged" case for null to collide with — the same reasoning that lets
 * {@code updateAutoApproveThresholdForCurrentTenant} use null to mean "off".
 */
public record BulkAssignLeadCampaignRequest(
        List<UUID> leadIds,
        UUID tenantCampaignId
) {
}
