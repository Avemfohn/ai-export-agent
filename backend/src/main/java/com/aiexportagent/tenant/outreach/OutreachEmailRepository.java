package com.aiexportagent.tenant.outreach;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutreachEmailRepository extends JpaRepository<OutreachEmail, UUID> {

    List<OutreachEmail> findByTenantId(UUID tenantId);

    /**
     * Tenant-scoped single-row lookup for client-facing endpoints, mirroring
     * {@code TenantLeadRepository.findByIdAndTenantId}. Used instead of a bare
     * {@code findById} so an id belonging to another tenant simply doesn't
     * match — the caller returns 404, which never reveals that the row exists.
     */
    Optional<OutreachEmail> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Deliberately NOT tenant-scoped — used only by
     * {@code OutreachSendingScheduler}, a system-internal job with no client
     * input, to pick the globally oldest QUEUED emails across all tenants
     * for throttled sending. Every actual write after selection still goes
     * through the normal tenant-scoped service methods.
     */
    List<OutreachEmail> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
