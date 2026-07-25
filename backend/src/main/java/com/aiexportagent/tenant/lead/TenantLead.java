package com.aiexportagent.tenant.lead;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Bridge table: tenant <-> global_supplier. Foreign keys are stored as plain
 * UUID columns rather than JPA relations to keep this skeleton simple.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenant_leads")
public class TenantLead extends Auditable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "global_supplier_id", nullable = false)
    private UUID globalSupplierId;

    @Column(name = "tenant_campaign_id")
    private UUID tenantCampaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private LeadStatus status;

    @Column(name = "qualification_score", precision = 5, scale = 2)
    private BigDecimal qualificationScore;

    @Column(name = "qualification_notes", columnDefinition = "text")
    private String qualificationNotes;

    // Simple String mapping for the JSONB column for this skeleton — no JSON<->POJO wiring yet.
    @Column(name = "ai_match_metadata", columnDefinition = "jsonb", nullable = false)
    private String aiMatchMetadata;
}
