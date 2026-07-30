package com.aiexportagent.ai.outreach;

import com.aiexportagent.ai.client.AiClient;
import com.aiexportagent.ai.client.AiEmailDraftRequest;
import com.aiexportagent.ai.client.AiEmailDraftResult;
import com.aiexportagent.ai.outreach.dto.OutreachDraftSummaryResponse;
import com.aiexportagent.global.contact.GlobalSupplierContact;
import com.aiexportagent.global.contact.GlobalSupplierContactService;
import com.aiexportagent.global.supplier.GlobalSupplier;
import com.aiexportagent.global.supplier.GlobalSupplierService;
import com.aiexportagent.tenant.account.TenantSettings;
import com.aiexportagent.tenant.account.TenantSettingsService;
import com.aiexportagent.tenant.campaign.TenantCampaign;
import com.aiexportagent.tenant.campaign.TenantCampaignService;
import com.aiexportagent.tenant.lead.LeadStatus;
import com.aiexportagent.tenant.lead.TenantLead;
import com.aiexportagent.tenant.lead.TenantLeadService;
import com.aiexportagent.tenant.outreach.OutreachEmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates AI outreach-email drafting for the current tenant: for every
 * APPROVED tenant_lead that doesn't already have an outreach_email, resolves
 * the applicable base draft template (campaign snapshot if the lead belongs
 * to one, else the tenant default), picks a contact, and has the AI
 * customize the template. Create-only / idempotent — never touches a lead
 * that already has an outreach_email.
 *
 * <p>Two entry points sharing the same candidate-finding + AI-drafting
 * logic, differing only in the resulting row's initial status:
 * {@link #draftForCurrentTenant()} (manual, via {@code POST
 * /api/outreach-emails/draft}, produces DRAFT — a testing/backfill escape
 * hatch) and {@link #queueForCurrentTenant()} (automated, via {@code
 * OutreachQueueingScheduler}, produces QUEUED directly — the real
 * approve-to-send pipeline, where DRAFT is deliberately never persisted).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutreachDraftingService {

    private final TenantSettingsService tenantSettingsService;
    private final TenantLeadService tenantLeadService;
    private final TenantCampaignService tenantCampaignService;
    private final GlobalSupplierService globalSupplierService;
    private final GlobalSupplierContactService globalSupplierContactService;
    private final OutreachEmailService outreachEmailService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    /**
     * Deliberately NOT @Transactional at this level — same reason as
     * {@code LeadScoringService.scoreForCurrentTenant}: the loop below makes
     * a synchronous external AI HTTP call per candidate, and holding one DB
     * transaction open across all of them would starve the connection pool
     * once real providers are wired in. Each read call below has its own
     * short transaction, and {@link OutreachEmailService#createDraftEmail}
     * opens its own REQUIRES_NEW transaction per email, committing
     * immediately.
     */
    public OutreachDraftSummaryResponse draftForCurrentTenant() {
        TenantSettings settings = tenantSettingsService.getForCurrentTenant();
        List<TenantLead> candidates = findUndraftedApprovedLeadsForCurrentTenant();

        int drafted = 0;
        int skippedNoContact = 0;
        int skippedNoTemplate = 0;
        int failed = 0;

        for (TenantLead lead : candidates) {
            try {
                DraftAttempt attempt = draftOneLead(settings, lead);
                if (attempt.skipReason() == SkipReason.NO_CONTACT) {
                    skippedNoContact++;
                    log.warn("Skipping outreach draft for lead {}: no contact on file", lead.getId());
                    continue;
                }
                if (attempt.skipReason() == SkipReason.NO_TEMPLATE) {
                    skippedNoTemplate++;
                    log.warn("Skipping outreach draft for lead {}: email template has no subject/body", lead.getId());
                    continue;
                }
                DraftedEmail draft = attempt.email();
                outreachEmailService.createDraftEmail(
                        draft.leadId(), draft.toEmail(), draft.subject(), draft.body());
                drafted++;
            } catch (Exception e) {
                // Catches both AI-call failures and DB-write failures — one bad lead
                // shouldn't abort drafts already committed for the others, since each
                // createDraftEmail() call commits independently.
                failed++;
                log.warn("Outreach drafting failed for lead {}: {}", lead.getId(), e.getMessage());
            }
        }

        return new OutreachDraftSummaryResponse(
                candidates.size(), drafted, skippedNoContact, skippedNoTemplate, failed);
    }

    /**
     * Same candidate-finding + AI-drafting logic as {@link #draftForCurrentTenant()},
     * but inserts each result directly as QUEUED via
     * {@link OutreachEmailService#createQueuedEmail} instead of DRAFT.
     * Called by {@code OutreachQueueingScheduler} — the automated path,
     * where DRAFT is deliberately never a persisted state (see CLAUDE.md's
     * automated outreach pipeline: "DRAFT is invisible in the automated flow").
     */
    public OutreachDraftSummaryResponse queueForCurrentTenant() {
        TenantSettings settings = tenantSettingsService.getForCurrentTenant();
        List<TenantLead> candidates = findUndraftedApprovedLeadsForCurrentTenant();

        int queued = 0;
        int skippedNoContact = 0;
        int skippedNoTemplate = 0;
        int failed = 0;

        for (TenantLead lead : candidates) {
            try {
                DraftAttempt attempt = draftOneLead(settings, lead);
                if (attempt.skipReason() == SkipReason.NO_CONTACT) {
                    skippedNoContact++;
                    log.warn("Skipping outreach queueing for lead {}: no contact on file", lead.getId());
                    continue;
                }
                if (attempt.skipReason() == SkipReason.NO_TEMPLATE) {
                    skippedNoTemplate++;
                    log.warn("Skipping outreach queueing for lead {}: email template has no subject/body",
                            lead.getId());
                    continue;
                }
                DraftedEmail draft = attempt.email();
                outreachEmailService.createQueuedEmail(
                        draft.leadId(), draft.toEmail(), draft.subject(), draft.body());
                queued++;
            } catch (Exception e) {
                failed++;
                log.warn("Outreach queueing failed for lead {}: {}", lead.getId(), e.getMessage());
            }
        }

        return new OutreachDraftSummaryResponse(
                candidates.size(), queued, skippedNoContact, skippedNoTemplate, failed);
    }

    private List<TenantLead> findUndraftedApprovedLeadsForCurrentTenant() {
        List<TenantLead> approvedLeads = tenantLeadService.findByStatusForCurrentTenant(LeadStatus.APPROVED);
        Set<UUID> alreadyDrafted = outreachEmailService.getLeadIdsWithOutreachForCurrentTenant();
        return approvedLeads.stream().filter(lead -> !alreadyDrafted.contains(lead.getId())).toList();
    }

    /** Why a lead produced no draft. Countable non-errors, distinct from a thrown exception (a hard failure). */
    private enum SkipReason {
        NO_CONTACT,
        NO_TEMPLATE
    }

    /** Exactly one of {@code email} / {@code skipReason} is non-null. */
    private record DraftAttempt(DraftedEmail email, SkipReason skipReason) {

        static DraftAttempt drafted(DraftedEmail email) {
            return new DraftAttempt(email, null);
        }

        static DraftAttempt skipped(SkipReason reason) {
            return new DraftAttempt(null, reason);
        }
    }

    private DraftAttempt draftOneLead(TenantSettings settings, TenantLead lead) {
        GlobalSupplier supplier = globalSupplierService.getById(lead.getGlobalSupplierId());
        Optional<GlobalSupplierContact> contact = pickContact(supplier.getId(), globalSupplierContactService);
        if (contact.isEmpty()) {
            return DraftAttempt.skipped(SkipReason.NO_CONTACT);
        }

        String templateJson = resolveTemplate(settings, lead);
        if (!hasUsableTemplate(templateJson)) {
            return DraftAttempt.skipped(SkipReason.NO_TEMPLATE);
        }

        AiEmailDraftRequest request = new AiEmailDraftRequest(
                templateJson,
                supplier.getCompanyName(),
                supplier.getDomain(),
                supplier.getSector(),
                supplier.getDescription(),
                contact.get().getFullName(),
                contact.get().getJobTitle(),
                settings.getEmailSenderName());

        AiEmailDraftResult result = aiClient.draftEmail(request);
        return DraftAttempt.drafted(
                new DraftedEmail(lead.getId(), contact.get().getEmail(), result.subject(), result.body()));
    }

    /**
     * Guards the blank-template hole: the column default is {@code '{}'}, and
     * the drafting clients read {@code path("subject").asText("")}, so an
     * unconfigured template silently yields an email with an empty subject and
     * body that the automated pipeline would then send. Checked here — after
     * template resolution but <em>before</em> the AI call — so it costs nothing
     * with a real provider.
     */
    private boolean hasUsableTemplate(String templateJson) {
        if (templateJson == null || templateJson.isBlank()) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(templateJson);
            return !node.path("subject").asText("").isBlank() && !node.path("body").asText("").isBlank();
        } catch (Exception e) {
            // Unparseable is also unusable; treated as a skip rather than a per-lead failure
            // so a broken template doesn't look like a transient AI error in the logs.
            return false;
        }
    }

    private record DraftedEmail(UUID leadId, String toEmail, String subject, String body) {
    }

    /** Prefers the primary contact, falls back to the first available one, else empty. */
    private static Optional<GlobalSupplierContact> pickContact(
            UUID globalSupplierId, GlobalSupplierContactService contactService) {
        List<GlobalSupplierContact> contacts = contactService.findByGlobalSupplierId(globalSupplierId);
        return contacts.stream()
                .filter(GlobalSupplierContact::isPrimary)
                .findFirst()
                .or(() -> contacts.stream().findFirst());
    }

    private String resolveTemplate(TenantSettings settings, TenantLead lead) {
        if (lead.getTenantCampaignId() != null) {
            TenantCampaign campaign = tenantCampaignService.getByIdForCurrentTenant(lead.getTenantCampaignId());
            return campaign.getEmailDraftTemplateSnapshot();
        }
        return settings.getEmailDraftTemplate();
    }
}
