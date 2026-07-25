package com.aiexportagent.tenant.account;

import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantSettingsService {

    private final TenantSettingsRepository tenantSettingsRepository;

    public TenantSettings getForCurrentTenant() {
        return tenantSettingsRepository.findByTenantId(TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Tenant settings not found for current tenant"));
    }
}
