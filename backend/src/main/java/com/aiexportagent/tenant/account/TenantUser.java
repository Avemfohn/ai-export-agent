package com.aiexportagent.tenant.account;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenant_users")
public class TenantUser extends Auditable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "email", length = 320, nullable = false)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    /** CHECK constraint values: OWNER, ADMIN, MEMBER. */
    @Column(name = "role", length = 30, nullable = false)
    private String role;

    /** CHECK constraint values: ACTIVE, INVITED, DISABLED. */
    @Column(name = "status", length = 30, nullable = false)
    private String status;
}
