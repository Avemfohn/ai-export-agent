package com.aiexportagent.tenant.lead.dto;

import com.aiexportagent.global.supplier.GlobalSupplier;
import com.aiexportagent.tenant.lead.LeadStatus;
import com.aiexportagent.tenant.lead.TenantLead;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Includes the joined GlobalSupplier's company_name/domain/country/sector
 * (fetched via GlobalSupplierService, not a JPA relation) so the frontend
 * has something useful to render for a lead.
 */
public record TenantLeadResponse(
        UUID id,
        UUID tenantCampaignId,
        LeadStatus status,
        BigDecimal qualificationScore,
        String qualificationNotes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID globalSupplierId,
        String companyName,
        String domain,
        String country,
        String sector
) {

    public static TenantLeadResponse from(TenantLead lead, GlobalSupplier supplier) {
        return new TenantLeadResponse(
                lead.getId(),
                lead.getTenantCampaignId(),
                lead.getStatus(),
                lead.getQualificationScore(),
                lead.getQualificationNotes(),
                lead.getCreatedAt(),
                lead.getUpdatedAt(),
                supplier.getId(),
                supplier.getCompanyName(),
                supplier.getDomain(),
                supplier.getCountry(),
                supplier.getSector()
        );
    }
}
