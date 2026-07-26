package com.aiexportagent.tenant.account.dto;

import com.aiexportagent.tenant.account.TenantSettings;

import java.math.BigDecimal;

/**
 * Minimal read shape for tenant_settings — only exposes the
 * auto_approve_threshold field this feature needs. The JSONB fields
 * (buyer_criteria, email_draft_template, etc.) have no frontend consumer
 * yet and are intentionally left out.
 */
public record TenantSettingsResponse(
        BigDecimal autoApproveThreshold
) {

    public static TenantSettingsResponse from(TenantSettings settings) {
        return new TenantSettingsResponse(settings.getAutoApproveThreshold());
    }
}
