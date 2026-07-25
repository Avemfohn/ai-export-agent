package com.aiexportagent.common.tenant;

import java.util.UUID;

/**
 * Request-scoped (thread-local) holder for the current tenant id. This is
 * the ONLY trusted source of tenantId for tenant-scoped repository/service
 * calls — never read tenantId from a controller's request params/body.
 *
 * <p>Populated by {@link TenantContextFilter} for every incoming request
 * (currently from a hardcoded dev tenant id — see CLAUDE.md Sprint 1 status).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    /**
     * @return the current tenant id
     * @throws IllegalStateException if no tenant id has been set on this thread —
     *         there is intentionally no silent fallback.
     */
    public static UUID get() {
        UUID tenantId = CURRENT_TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext has not been set for the current thread");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
    }
}
