package com.aiexportagent.tenant.scraping;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "scraping_jobs")
public class ScrapingJob extends Auditable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_campaign_id")
    private UUID tenantCampaignId;

    /** CHECK constraint values: GOOGLE_MAPS, B2B_DIRECTORY, TRADE_FAIR_UPLOAD, MANUAL. */
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    /** CHECK constraint values: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED. */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    // Simple String mapping for the JSONB columns for this skeleton — no JSON<->POJO wiring yet.
    @Column(name = "params", columnDefinition = "jsonb", nullable = false)
    private String params;

    @Column(name = "result_summary", columnDefinition = "jsonb", nullable = false)
    private String resultSummary;

    @Column(name = "companies_found", nullable = false)
    private int companiesFound;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
