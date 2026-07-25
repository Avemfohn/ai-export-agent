package com.aiexportagent.global.contact;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * SHARED POOL entity — no tenant_id. See CLAUDE.md "Master Pool Architecture".
 * Foreign key to global_suppliers is stored as a plain UUID column rather
 * than a JPA relation, to keep this skeleton simple.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "global_supplier_contacts")
public class GlobalSupplierContact extends Auditable {

    @Column(name = "global_supplier_id", nullable = false)
    private UUID globalSupplierId;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "job_title", length = 255)
    private String jobTitle;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "linkedin_url", length = 1000)
    private String linkedinUrl;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "confidence_score", precision = 4, scale = 3)
    private BigDecimal confidenceScore;

    /** CHECK constraint values: WEBSITE_SCRAPE, ENRICHMENT_API, MANUAL. */
    @Column(name = "source", length = 50, nullable = false)
    private String source;
}
