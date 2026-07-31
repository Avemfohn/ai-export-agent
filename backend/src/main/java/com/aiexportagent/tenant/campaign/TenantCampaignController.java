package com.aiexportagent.tenant.campaign;

import com.aiexportagent.tenant.campaign.dto.CreateTenantCampaignRequest;
import com.aiexportagent.tenant.campaign.dto.TenantCampaignResponse;
import com.aiexportagent.tenant.campaign.dto.UpdateCampaignStatusRequest;
import com.aiexportagent.tenant.campaign.dto.UpdateTenantCampaignRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * tenantId is never accepted from the client — TenantCampaignService pulls
 * it from TenantContext. The {@code id} path variable is likewise resolved
 * through a tenant-scoped lookup, so another tenant's id 404s.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns")
public class TenantCampaignController {

    private final TenantCampaignService tenantCampaignService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public List<TenantCampaignResponse> list() {
        return tenantCampaignService.listForCurrentTenant().stream()
                .map(campaign -> TenantCampaignResponse.from(campaign, objectMapper))
                .toList();
    }

    @GetMapping("/{id}")
    public TenantCampaignResponse getById(@PathVariable UUID id) {
        return TenantCampaignResponse.from(tenantCampaignService.getByIdForCurrentTenant(id), objectMapper);
    }

    /**
     * The first resource-creating POST in this codebase — the existing POSTs
     * are batch actions rather than creation, so they return 200. This returns
     * 201.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantCampaignResponse create(@RequestBody CreateTenantCampaignRequest request) {
        return TenantCampaignResponse.from(tenantCampaignService.createForCurrentTenant(request), objectMapper);
    }

    /** Full replacement of the editable content; status has its own endpoint. */
    @PutMapping("/{id}")
    public TenantCampaignResponse update(
            @PathVariable UUID id, @RequestBody UpdateTenantCampaignRequest request) {
        return TenantCampaignResponse.from(
                tenantCampaignService.updateForCurrentTenant(id, request), objectMapper);
    }

    /** 409 if the transition isn't legal — see {@link CampaignStatus#canTransitionTo}. */
    @PatchMapping("/{id}/status")
    public TenantCampaignResponse updateStatus(
            @PathVariable UUID id, @RequestBody UpdateCampaignStatusRequest request) {
        return TenantCampaignResponse.from(
                tenantCampaignService.updateStatusForCurrentTenant(id, request.status()), objectMapper);
    }
}
