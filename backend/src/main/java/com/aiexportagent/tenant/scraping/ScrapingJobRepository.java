package com.aiexportagent.tenant.scraping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScrapingJobRepository extends JpaRepository<ScrapingJob, UUID> {

    List<ScrapingJob> findByTenantId(UUID tenantId);
}
