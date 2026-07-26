package com.aiexportagent.tenant.lead;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantLeadRepository extends JpaRepository<TenantLead, UUID> {

    List<TenantLead> findByTenantId(UUID tenantId);

    Optional<TenantLead> findByIdAndTenantId(UUID id, UUID tenantId);

    List<TenantLead> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId);
}
