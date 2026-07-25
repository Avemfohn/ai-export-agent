package com.aiexportagent.tenant.campaign;

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
@Table(name = "tenant_campaigns")
public class TenantCampaign extends Auditable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** CHECK constraint values: DRAFT, ACTIVE, PAUSED, COMPLETED, ARCHIVED. */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    // Simple String mapping for the JSONB column for this skeleton — no JSON<->POJO wiring yet.
    @Column(name = "buyer_criteria_snapshot", columnDefinition = "jsonb", nullable = false)
    private String buyerCriteriaSnapshot;

    /**
     * Per-campaign override/snapshot of the tenant's default outreach draft
     * template (see {@link com.aiexportagent.tenant.account.TenantSettings#getEmailDraftTemplate()}),
     * for campaigns targeting a different product line or region than the
     * tenant default. See CLAUDE.md.
     */
    @Column(name = "email_draft_template_snapshot", columnDefinition = "jsonb", nullable = false)
    private String emailDraftTemplateSnapshot;
}
