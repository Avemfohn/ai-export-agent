package com.aiexportagent.tenant.lead;

import com.aiexportagent.tenant.lead.dto.TenantLeadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * tenantId is never accepted from the client — TenantLeadService pulls it
 * from TenantContext.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leads")
public class TenantLeadController {

    private final TenantLeadService tenantLeadService;

    @GetMapping
    public List<TenantLeadResponse> list() {
        return tenantLeadService.listForCurrentTenant();
    }

    @GetMapping("/{id}")
    public TenantLeadResponse getById(@PathVariable UUID id) {
        return tenantLeadService.getByIdForCurrentTenant(id);
    }
}
