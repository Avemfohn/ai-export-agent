package com.aiexportagent.tenant.account;

import com.aiexportagent.tenant.account.dto.TenantSettingsResponse;
import com.aiexportagent.tenant.account.dto.UpdateAutoApproveThresholdRequest;
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

    @GetMapping
    public TenantSettingsResponse get() {
        return TenantSettingsResponse.from(tenantSettingsService.getForCurrentTenant());
    }

    @PatchMapping("/auto-approve-threshold")
    public TenantSettingsResponse updateAutoApproveThreshold(@RequestBody UpdateAutoApproveThresholdRequest request) {
        return TenantSettingsResponse.from(
                tenantSettingsService.updateAutoApproveThresholdForCurrentTenant(request.autoApproveThreshold()));
    }
}
