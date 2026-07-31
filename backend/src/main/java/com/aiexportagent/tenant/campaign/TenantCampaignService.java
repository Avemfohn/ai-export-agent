package com.aiexportagent.tenant.campaign;

import com.aiexportagent.common.exception.ApiException;
import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.common.validation.SettingsJsonValidator;
import com.aiexportagent.tenant.account.TenantSettings;
import com.aiexportagent.tenant.account.TenantSettingsService;
import com.aiexportagent.tenant.campaign.dto.CreateTenantCampaignRequest;
import com.aiexportagent.tenant.campaign.dto.UpdateTenantCampaignRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Owns tenant_campaigns.
 *
 * <p><strong>Dependency direction: lead → campaign, never the reverse.</strong>
 * {@code TenantLeadService} depends on this class (to validate a campaign id on
 * assignment) and {@code OutreachDraftingService} depends on both, so this
 * class must never depend on {@code TenantLeadService}. Spring Boot runs with
 * {@code allow-circular-references=false}, so closing that loop is a hard boot
 * failure rather than a subtle bug. If campaign screens need server-side lead
 * data later, give that its own bean instead of adding the edge here.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantCampaignService {

    private static final int MAX_NAME_CHARS = 255;
    private static final int MAX_DESCRIPTION_CHARS = 4000;
    private static final String EMPTY_JSON_OBJECT = "{}";

    private final TenantCampaignRepository tenantCampaignRepository;
    private final TenantSettingsService tenantSettingsService;
    private final ObjectMapper objectMapper;

    public List<TenantCampaign> listForCurrentTenant() {
        return tenantCampaignRepository.findByTenantId(TenantContext.get());
    }

    public TenantCampaign getByIdForCurrentTenant(UUID id) {
        return tenantCampaignRepository.findByIdAndTenantId(id, TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + id));
    }

    /**
     * Batch variant of {@link #getByIdForCurrentTenant}, keyed by id. Ids that
     * don't belong to the current tenant simply don't appear in the map, so
     * callers must treat a missing key as "not available" rather than assuming
     * presence.
     */
    public Map<UUID, TenantCampaign> findByIdsForCurrentTenant(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return tenantCampaignRepository.findByIdInAndTenantId(ids, TenantContext.get()).stream()
                .collect(Collectors.toMap(TenantCampaign::getId, Function.identity()));
    }

    /**
     * The campaign's email template starts as a copy of the tenant's current
     * default — that's what "snapshot" means, and it lets the editor open on
     * something usable rather than an empty form.
     *
     * <p>The copied value is deliberately NOT validated: a tenant who hasn't
     * configured a template yet holds {@code '{}'}, and validating it would
     * make their very first campaign impossible to create. Only client-supplied
     * values are validated.
     */
    @Transactional
    public TenantCampaign createForCurrentTenant(CreateTenantCampaignRequest request) {
        String name = requireValidName(request.name());
        String description = validateDescription(request.description());

        CampaignStatus status = request.status() == null ? CampaignStatus.DRAFT : request.status();
        if (status != CampaignStatus.DRAFT && status != CampaignStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A new campaign can only start as DRAFT or ACTIVE, got: " + status);
        }

        TenantCampaign campaign = new TenantCampaign();
        campaign.setTenantId(TenantContext.get());
        campaign.setName(name);
        campaign.setDescription(description);
        campaign.setStatus(status);
        // Both columns are NOT NULL, so Hibernate rejects nulls before Postgres could
        // ever apply its defaults — set them explicitly.
        campaign.setBuyerCriteriaSnapshot(EMPTY_JSON_OBJECT);
        campaign.setEmailDraftTemplateSnapshot(resolveStartingTemplate());

        return tenantCampaignRepository.save(campaign);
    }

    /** Full replacement — see {@link UpdateTenantCampaignRequest} for why PUT, not PATCH. */
    @Transactional
    public TenantCampaign updateForCurrentTenant(UUID id, UpdateTenantCampaignRequest request) {
        TenantCampaign campaign = getByIdForCurrentTenant(id);

        campaign.setName(requireValidName(request.name()));
        campaign.setDescription(validateDescription(request.description()));

        if (request.emailDraftTemplate() != null) {
            SettingsJsonValidator.validateEmailDraftTemplate(request.emailDraftTemplate(), "Email template");
            campaign.setEmailDraftTemplateSnapshot(write(request.emailDraftTemplate()));
        }

        return tenantCampaignRepository.save(campaign);
    }

    /** Narrow status gate; the transition rules live on {@link CampaignStatus}. */
    @Transactional
    public TenantCampaign updateStatusForCurrentTenant(UUID id, CampaignStatus newStatus) {
        if (newStatus == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status is required");
        }
        TenantCampaign campaign = getByIdForCurrentTenant(id);
        if (!campaign.getStatus().canTransitionTo(newStatus)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot change campaign status from " + campaign.getStatus() + " to " + newStatus);
        }
        campaign.setStatus(newStatus);
        return tenantCampaignRepository.save(campaign);
    }

    private String resolveStartingTemplate() {
        try {
            TenantSettings settings = tenantSettingsService.getForCurrentTenant();
            String template = settings.getEmailDraftTemplate();
            return template == null || template.isBlank() ? EMPTY_JSON_OBJECT : template;
        } catch (NotFoundException e) {
            // A tenant with no settings row yet can still create campaigns.
            return EMPTY_JSON_OBJECT;
        }
    }

    private String requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Campaign name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_CHARS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Campaign name must be " + MAX_NAME_CHARS + " characters or fewer");
        }
        return trimmed;
    }

    /** Null/blank is legal — the column is nullable and "no description" is a real state. */
    private String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        if (description.length() > MAX_DESCRIPTION_CHARS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Campaign description must be " + MAX_DESCRIPTION_CHARS + " characters or fewer");
        }
        return description;
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not store the provided JSON value");
        }
    }
}
