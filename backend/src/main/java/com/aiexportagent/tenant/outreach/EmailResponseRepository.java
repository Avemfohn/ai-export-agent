package com.aiexportagent.tenant.outreach;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailResponseRepository extends JpaRepository<EmailResponse, UUID> {

    List<EmailResponse> findByTenantId(UUID tenantId);
}
