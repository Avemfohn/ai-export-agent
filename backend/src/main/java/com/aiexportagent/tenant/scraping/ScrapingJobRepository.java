package com.aiexportagent.tenant.scraping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScrapingJobRepository extends JpaRepository<ScrapingJob, UUID> {

    List<ScrapingJob> findByTenantId(UUID tenantId);

    /**
     * Tenant-scoped lookup. Never use {@code findById} for a job reached from a
     * request — the id would be client-supplied and could name another tenant's
     * job.
     */
    Optional<ScrapingJob> findByIdAndTenantId(UUID id, UUID tenantId);
}
