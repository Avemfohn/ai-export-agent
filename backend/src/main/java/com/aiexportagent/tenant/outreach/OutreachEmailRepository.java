package com.aiexportagent.tenant.outreach;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutreachEmailRepository extends JpaRepository<OutreachEmail, UUID> {

    List<OutreachEmail> findByTenantId(UUID tenantId);

    /**
     * Deliberately NOT tenant-scoped — used only by
     * {@code OutreachSendingScheduler}, a system-internal job with no client
     * input, to pick the globally oldest QUEUED emails across all tenants
     * for throttled sending. Every actual write after selection still goes
     * through the normal tenant-scoped service methods.
     */
    List<OutreachEmail> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
