package com.aiexportagent.tenant.account;

import com.aiexportagent.common.exception.ApiException;
import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.common.validation.SettingsJsonValidator;
import com.aiexportagent.tenant.account.dto.UpdateTenantSettingsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantSettingsService {

    private final TenantSettingsRepository tenantSettingsRepository;
    private final ObjectMapper objectMapper;

    public TenantSettings getForCurrentTenant() {
        return tenantSettingsRepository.findByTenantId(TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Tenant settings not found for current tenant"));
    }

    /**
     * Same as {@link #getForCurrentTenant()} but materialises a row from the
     * column defaults instead of 404-ing, so the settings page works for a
     * tenant that has no row yet. Every tenant_settings row today comes from
     * Flyway seed data, so this only matters once tenants are created any other
     * way.
     *
     * <p>Scoped to the read path deliberately: the schedulers still use
     * {@link #getForCurrentTenant()} and log a per-tenant warning instead, since
     * a background job silently creating configuration rows is worse than it
     * skipping a tenant that isn't set up yet.
     *
     * <p>Note this makes GET non-idempotent — see the caller in
     * {@code TenantSettingsController}.
     *
     * <p>Known limitation: check-then-insert. Two concurrent first-loads for the
     * same settings-less tenant would both insert, and
     * {@code uq_tenant_settings_tenant} would reject the loser with a 500. Left
     * unhandled deliberately — the window only exists for a tenant with no row,
     * and every tenant is currently created by Flyway with one. Recovering
     * properly needs a separate transaction (a {@code REQUIRES_NEW} method here
     * would be self-invoked and silently bypass the proxy), which isn't worth a
     * dedicated component for a path that can't be reached until tenant signup
     * exists.
     */
    @Transactional
    public TenantSettings getOrCreateForCurrentTenant() {
        UUID tenantId = TenantContext.get();
        return tenantSettingsRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantSettings settings = new TenantSettings();
                    settings.setTenantId(tenantId);
                    settings.setBuyerCriteria("{}");
                    settings.setTargetSectors("[]");
                    settings.setTargetRegions("[]");
                    settings.setNotificationPrefs("{}");
                    settings.setEmailDraftTemplate("{}");
                    return tenantSettingsRepository.save(settings);
                });
    }

    /**
     * Partial update: a null field means "leave unchanged" (see
     * {@link UpdateTenantSettingsRequest} for why that's unambiguous here).
     * Every provided field is shape-validated before the write — these columns
     * feed the AI scoring prompt and the outreach drafter directly, where bad
     * values fail silently rather than loudly.
     */
    @Transactional
    public TenantSettings updateForCurrentTenant(UpdateTenantSettingsRequest request) {
        TenantSettings settings = getForCurrentTenant();

        if (request.buyerCriteria() != null) {
            SettingsJsonValidator.validateBuyerCriteria(request.buyerCriteria(), "Buyer criteria");
            settings.setBuyerCriteria(write(request.buyerCriteria()));
        }
        if (request.targetSectors() != null) {
            SettingsJsonValidator.validateStringArray(request.targetSectors(), "Target sectors");
            settings.setTargetSectors(write(request.targetSectors()));
        }
        if (request.targetRegions() != null) {
            SettingsJsonValidator.validateStringArray(request.targetRegions(), "Target regions");
            settings.setTargetRegions(write(request.targetRegions()));
        }
        if (request.emailDraftTemplate() != null) {
            SettingsJsonValidator.validateEmailDraftTemplate(request.emailDraftTemplate(), "Email template");
            settings.setEmailDraftTemplate(write(request.emailDraftTemplate()));
        }

        return tenantSettingsRepository.save(settings);
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            // Re-serializing a node Jackson just parsed shouldn't fail; treat as a bad request rather than a 500.
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not store the provided JSON value");
        }
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
