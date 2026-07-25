package com.aiexportagent.global.supplier;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * SHARED POOL entity — no tenant_id. Deduped by domain across ALL tenants.
 * See CLAUDE.md "Master Pool Architecture".
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "global_suppliers")
public class GlobalSupplier extends Auditable {

    @Column(name = "company_name", length = 500, nullable = false)
    private String companyName;

    @Column(name = "domain", length = 500, nullable = false, unique = true)
    private String domain;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "sector", length = 255)
    private String sector;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** CHECK constraint values: GOOGLE_MAPS, B2B_DIRECTORY, TRADE_FAIR_UPLOAD, MANUAL. */
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    /** Simple String mapping for the JSONB column for this skeleton — no JSON<->POJO wiring yet. */
    @Column(name = "enrichment_data", columnDefinition = "jsonb", nullable = false)
    private String enrichmentData;

    @Column(name = "last_scraped_at")
    private OffsetDateTime lastScrapedAt;
}
