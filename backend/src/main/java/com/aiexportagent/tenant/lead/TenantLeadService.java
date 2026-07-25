package com.aiexportagent.tenant.lead;

import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.global.supplier.GlobalSupplier;
import com.aiexportagent.global.supplier.GlobalSupplierService;
import com.aiexportagent.tenant.lead.dto.TenantLeadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantLeadService {

    private final TenantLeadRepository tenantLeadRepository;
    private final GlobalSupplierService globalSupplierService;

    public List<TenantLeadResponse> listForCurrentTenant() {
        UUID tenantId = TenantContext.get();
        return tenantLeadRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TenantLeadResponse getByIdForCurrentTenant(UUID id) {
        TenantLead lead = tenantLeadRepository.findByIdAndTenantId(id, TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Lead not found: " + id));
        return toResponse(lead);
    }

    /** Used by the AI scoring engine to skip global_suppliers already linked to this tenant. */
    public Set<UUID> getLinkedGlobalSupplierIdsForCurrentTenant() {
        return tenantLeadRepository.findByTenantId(TenantContext.get()).stream()
                .map(TenantLead::getGlobalSupplierId)
                .collect(Collectors.toSet());
    }

    /**
     * Creates a new tenant_lead for the current tenant from an AI scoring
     * result. tenantId is always taken from {@link TenantContext}, never a
     * parameter — same rule as every other write path in this codebase.
     */
    @Transactional
    public TenantLead createAiScoredLead(
            UUID globalSupplierId,
            LeadStatus status,
            BigDecimal qualificationScore,
            String qualificationNotes,
            String aiMatchMetadataJson) {
        TenantLead lead = new TenantLead();
        lead.setTenantId(TenantContext.get());
        lead.setGlobalSupplierId(globalSupplierId);
        lead.setStatus(status);
        lead.setQualificationScore(qualificationScore);
        lead.setQualificationNotes(qualificationNotes);
        lead.setAiMatchMetadata(aiMatchMetadataJson);
        return tenantLeadRepository.save(lead);
    }

    private TenantLeadResponse toResponse(TenantLead lead) {
        GlobalSupplier supplier = globalSupplierService.getById(lead.getGlobalSupplierId());
        return TenantLeadResponse.from(lead, supplier);
    }
}
