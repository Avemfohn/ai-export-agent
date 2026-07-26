package com.aiexportagent.tenant.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Thin owning-Service for {@link TenantRepository}, per this codebase's
 * "repository never injected into another package's service" convention —
 * didn't exist before because nothing needed cross-tenant iteration until
 * now. Used only by {@code OutreachQueueingScheduler} to loop over every
 * tenant (a system-internal job with no request/client input, so there's no
 * "current tenant" to scope a query by).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;

    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }
}
