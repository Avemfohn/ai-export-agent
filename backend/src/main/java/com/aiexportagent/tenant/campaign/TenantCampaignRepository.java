package com.aiexportagent.tenant.campaign;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantCampaignRepository extends JpaRepository<TenantCampaign, UUID> {

    List<TenantCampaign> findByTenantId(UUID tenantId);

    Optional<TenantCampaign> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Batch lookup, tenant-scoped like every other query here. Used by the
     * outreach drafter to resolve every candidate lead's campaign in one query
     * instead of one per lead.
     */
    List<TenantCampaign> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId);
}
