package com.aiexportagent.tenant.lead;

import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.global.supplier.GlobalSupplier;
import com.aiexportagent.global.supplier.GlobalSupplierService;
import com.aiexportagent.tenant.lead.dto.TenantLeadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    private TenantLeadResponse toResponse(TenantLead lead) {
        GlobalSupplier supplier = globalSupplierService.getById(lead.getGlobalSupplierId());
        return TenantLeadResponse.from(lead, supplier);
    }
}
