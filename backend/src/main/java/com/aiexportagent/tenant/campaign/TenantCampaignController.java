package com.aiexportagent.tenant.campaign;

import com.aiexportagent.tenant.campaign.dto.TenantCampaignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * tenantId is never accepted from the client — TenantCampaignService pulls
 * it from TenantContext.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns")
public class TenantCampaignController {

    private final TenantCampaignService tenantCampaignService;

    @GetMapping
    public List<TenantCampaignResponse> list() {
        return tenantCampaignService.listForCurrentTenant().stream()
                .map(TenantCampaignResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TenantCampaignResponse getById(@PathVariable UUID id) {
        return TenantCampaignResponse.from(tenantCampaignService.getByIdForCurrentTenant(id));
    }
}
