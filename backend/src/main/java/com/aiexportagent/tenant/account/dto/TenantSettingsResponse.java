package com.aiexportagent.tenant.account.dto;

import com.aiexportagent.tenant.account.TenantSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * Read shape for tenant_settings.
 *
 * <p>The JSONB columns are returned as real JSON ({@link JsonNode}), not as
 * escaped strings, so the client gets objects/arrays it can edit directly.
 * {@code TenantCampaignResponse} still exposes its snapshot columns as
 * {@code String} — that precedent was set while nothing consumed them, and
 * should be migrated to match when campaign editing lands.
 *
 * <p>{@code emailSenderName}/{@code emailSenderAddress} are exposed
 * <strong>read-only</strong>: the customer needs to know which address their
 * outreach comes from (and {@code emailSenderName} fills the
 * {@code {{senderName}}} placeholder in every drafted email), but these are
 * coupled to SPF/DKIM and domain reputation, so they're configured at
 * onboarding rather than edited here. {@link UpdateTenantSettingsRequest}
 * intentionally cannot write them.
 */
public record TenantSettingsResponse(
        JsonNode buyerCriteria,
        JsonNode targetSectors,
        JsonNode targetRegions,
        JsonNode emailDraftTemplate,
        String emailSenderName,
        String emailSenderAddress,
        BigDecimal autoApproveThreshold
) {

    public static TenantSettingsResponse from(TenantSettings settings, ObjectMapper objectMapper) {
        return new TenantSettingsResponse(
                readOrNull(settings.getBuyerCriteria(), objectMapper),
                readOrNull(settings.getTargetSectors(), objectMapper),
                readOrNull(settings.getTargetRegions(), objectMapper),
                readOrNull(settings.getEmailDraftTemplate(), objectMapper),
                settings.getEmailSenderName(),
                settings.getEmailSenderAddress(),
                settings.getAutoApproveThreshold()
        );
    }

    /**
     * Legacy rows predate write-time validation, so a stored value could in
     * principle be unparseable. Degrade that single field to null rather than
     * 500-ing the whole settings page — the client already has to treat these
     * as possibly-absent (Jackson omits nulls).
     */
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
