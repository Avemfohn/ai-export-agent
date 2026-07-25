package com.aiexportagent.tenant.campaign;

import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantCampaignService {

    private final TenantCampaignRepository tenantCampaignRepository;

    public List<TenantCampaign> listForCurrentTenant() {
        return tenantCampaignRepository.findByTenantId(TenantContext.get());
    }

    public TenantCampaign getByIdForCurrentTenant(UUID id) {
        return tenantCampaignRepository.findByIdAndTenantId(id, TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + id));
    }
}
