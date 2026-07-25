package com.aiexportagent.tenant.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {

    List<TenantUser> findByTenantId(UUID tenantId);
}
