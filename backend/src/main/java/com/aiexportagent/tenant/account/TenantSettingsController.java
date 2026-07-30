package com.aiexportagent.tenant.account;

import com.aiexportagent.tenant.account.dto.TenantSettingsResponse;
import com.aiexportagent.tenant.account.dto.UpdateAutoApproveThresholdRequest;
import com.aiexportagent.tenant.account.dto.UpdateTenantSettingsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * tenantId is never accepted from the client — TenantSettingsService pulls
 * it from TenantContext.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tenant-settings")
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;
    private final ObjectMapper objectMapper;

    /**
     * Note: deliberately non-idempotent — this materialises a settings row from
     * the column defaults if the tenant has none, so the settings page works
     * before anything has been configured. Don't route it to a read replica.
     */
    @GetMapping
    public TenantSettingsResponse get() {
        return TenantSettingsResponse.from(tenantSettingsService.getOrCreateForCurrentTenant(), objectMapper);
    }

    /**
     * Partial update of the free-form configuration columns. Deliberately
     * cannot write the sender identity fields — see
     * {@link UpdateTenantSettingsRequest}.
     */
    @PatchMapping
    public TenantSettingsResponse update(@RequestBody UpdateTenantSettingsRequest request) {
        return TenantSettingsResponse.from(tenantSettingsService.updateForCurrentTenant(request), objectMapper);
    }

    /**
     * Kept separate from {@link #update(UpdateTenantSettingsRequest)}: this is
     * the one settings field where {@code null} is a meaningful value
     * ("auto-approve off") rather than "leave unchanged".
     */
    @PatchMapping("/auto-approve-threshold")
    public TenantSettingsResponse updateAutoApproveThreshold(@RequestBody UpdateAutoApproveThresholdRequest request) {
        return TenantSettingsResponse.from(
                tenantSettingsService.updateAutoApproveThresholdForCurrentTenant(request.autoApproveThreshold()),
                objectMapper);
    }
}
