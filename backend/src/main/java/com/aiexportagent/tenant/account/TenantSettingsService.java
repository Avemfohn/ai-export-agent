package com.aiexportagent.tenant.account;

import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantSettingsService {

    private final TenantSettingsRepository tenantSettingsRepository;

    public TenantSettings getForCurrentTenant() {
        return tenantSettingsRepository.findByTenantId(TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Tenant settings not found for current tenant"));
    }

    /**
     * Sets or clears (via {@code null}) the current tenant's auto-approve
     * score threshold. No cross-validation against
     * {@code app.ai.match-threshold} — see the Javadoc on
     * {@link com.aiexportagent.ai.scoring.LeadScoringService} for why a
     * threshold set below the match threshold is allowed.
     */
    @Transactional
    public TenantSettings updateAutoApproveThresholdForCurrentTenant(BigDecimal threshold) {
        TenantSettings settings = getForCurrentTenant();
        settings.setAutoApproveThreshold(threshold);
        return tenantSettingsRepository.save(settings);
    }
}
