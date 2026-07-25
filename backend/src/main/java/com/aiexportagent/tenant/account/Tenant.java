package com.aiexportagent.tenant.account;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The tenant root entity itself — its id IS the tenantId used everywhere
 * else via TenantContext, so unlike every other entity in the tenant/
 * package it has no separate tenant_id column to filter by.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenants")
public class Tenant extends Auditable {

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "slug", length = 100, nullable = false, unique = true)
    private String slug;

    /** CHECK constraint values: ACTIVE, SUSPENDED, TRIAL, CANCELLED. */
    @Column(name = "status", length = 30, nullable = false)
    private String status;
}
