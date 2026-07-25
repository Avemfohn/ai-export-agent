package com.aiexportagent.tenant.campaign;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantCampaignRepository extends JpaRepository<TenantCampaign, UUID> {

    List<TenantCampaign> findByTenantId(UUID tenantId);

    Optional<TenantCampaign> findByIdAndTenantId(UUID id, UUID tenantId);
}
