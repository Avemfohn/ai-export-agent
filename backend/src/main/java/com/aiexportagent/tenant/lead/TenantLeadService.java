package com.aiexportagent.tenant.lead;

import com.aiexportagent.common.exception.ApiException;
import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.global.supplier.GlobalSupplier;
import com.aiexportagent.global.supplier.GlobalSupplierService;
import com.aiexportagent.tenant.lead.dto.BulkUpdateLeadStatusResponse;
import com.aiexportagent.tenant.lead.dto.TenantLeadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

    /**
     * Minimal approve/reject review gate: only accepts APPROVED/REJECTED,
     * and only from a lead currently PENDING_APPROVAL — this is a review
     * step for a freshly AI-scored match, not a general status editor.
     */
    @Transactional
    public TenantLeadResponse updateStatusForCurrentTenant(UUID id, LeadStatus newStatus) {
        if (newStatus != LeadStatus.APPROVED && newStatus != LeadStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "status must be APPROVED or REJECTED, got: " + newStatus);
        }
        TenantLead lead = tenantLeadRepository.findByIdAndTenantId(id, TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Lead not found: " + id));
        if (lead.getStatus() != LeadStatus.PENDING_APPROVAL) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Lead " + id + " is " + lead.getStatus() + ", not PENDING_APPROVAL — cannot approve/reject");
        }
        lead.setStatus(newStatus);
        return toResponse(tenantLeadRepository.save(lead));
    }

    /**
     * Bulk approve/reject for the review-at-scale flow: processes what it
     * can and reports counts rather than an all-or-nothing 409, matching
     * every other batch operation in this codebase
     * ({@code LeadScoringService}, {@code OutreachDraftingService}). Tenant
     * isolation is structural, not an extra check — {@code leadIds} is
     * never trusted: only ids returned by {@code findByIdInAndTenantId} for
     * the CURRENT tenant are candidates for update, so an id belonging to
     * another tenant (or that doesn't exist) simply never matches and is
     * counted in {@code notFoundOrForeignTenant}, never processed.
     *
     * <p>Single {@code @Transactional} (not per-item REQUIRES_NEW like
     * {@link #createAiScoredLead}) is correct here — unlike AI scoring/
     * drafting, there's no external HTTP call in this loop, just fast
     * in-DB status flips, so nothing benefits from a shorter transaction.
     */
    @Transactional
    public BulkUpdateLeadStatusResponse bulkUpdateStatusForCurrentTenant(List<UUID> leadIds, LeadStatus newStatus) {
        if (newStatus != LeadStatus.APPROVED && newStatus != LeadStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "status must be APPROVED or REJECTED, got: " + newStatus);
        }
        if (leadIds == null || leadIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "leadIds must be a non-empty list");
        }

        List<TenantLead> owned = tenantLeadRepository.findByIdInAndTenantId(leadIds, TenantContext.get());
        Map<UUID, TenantLead> byId = owned.stream().collect(Collectors.toMap(TenantLead::getId, l -> l));

        int updated = 0;
        int skippedWrongStatus = 0;
        int notFoundOrForeignTenant = 0;

        for (UUID id : leadIds) {
            TenantLead lead = byId.get(id);
            if (lead == null) {
                notFoundOrForeignTenant++;
                continue;
            }
            if (lead.getStatus() != LeadStatus.PENDING_APPROVAL) {
                skippedWrongStatus++;
                continue;
            }
            lead.setStatus(newStatus);
            tenantLeadRepository.save(lead);
            updated++;
        }

        return new BulkUpdateLeadStatusResponse(leadIds.size(), updated, skippedWrongStatus, notFoundOrForeignTenant);
    }

    /** Used by the AI scoring engine to skip global_suppliers already linked to this tenant. */
    public Set<UUID> getLinkedGlobalSupplierIdsForCurrentTenant() {
        return tenantLeadRepository.findByTenantId(TenantContext.get()).stream()
                .map(TenantLead::getGlobalSupplierId)
                .collect(Collectors.toSet());
    }

    /** Used by the AI outreach-drafting engine to find eligible leads. */
    public List<TenantLead> findByStatusForCurrentTenant(LeadStatus status) {
        return tenantLeadRepository.findByTenantId(TenantContext.get()).stream()
                .filter(lead -> lead.getStatus() == status)
                .toList();
    }

    /**
     * Defense-in-depth ownership guard: throws if {@code leadId} doesn't
     * exist or doesn't belong to the current tenant. Used by
     * {@code OutreachEmailService.createDraftEmail} before inserting an
     * outreach_email against a tenant_lead_id — that id is expected to
     * already be tenant-safe by construction (sourced from
     * {@link #findByStatusForCurrentTenant}), but this makes the guarantee
     * a real runtime check rather than relying solely on every future
     * caller being equally careful.
     */
    public void assertOwnedByCurrentTenant(UUID leadId) {
        tenantLeadRepository.findByIdAndTenantId(leadId, TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));
    }

    /**
     * Creates a new tenant_lead for the current tenant from an AI scoring
     * result. tenantId is always taken from {@link TenantContext}, never a
     * parameter — same rule as every other write path in this codebase.
     *
     * <p>REQUIRES_NEW: always its own short transaction, committed
     * immediately, regardless of whether the caller happens to be running
     * inside an ambient transaction — {@code LeadScoringService} calls this
     * once per candidate outside any transaction of its own specifically so
     * a slow external AI call never holds a DB connection open.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    /**
     * Marks a lead EMAIL_SENT after a successful send — called by
     * {@code OutreachSendingScheduler} (system-internal, not client input),
     * with {@link TenantContext} already set to this lead's own tenant.
     * Guarded to only apply from APPROVED, same "only sensible transition"
     * discipline as every other status-writing method here, even though
     * this isn't reachable from a client request.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEmailSentForCurrentTenant(UUID leadId) {
        TenantLead lead = tenantLeadRepository.findByIdAndTenantId(leadId, TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));
        if (lead.getStatus() == LeadStatus.APPROVED) {
            lead.setStatus(LeadStatus.EMAIL_SENT);
            tenantLeadRepository.save(lead);
        }
    }

    private TenantLeadResponse toResponse(TenantLead lead) {
        GlobalSupplier supplier = globalSupplierService.getById(lead.getGlobalSupplierId());
        return TenantLeadResponse.from(lead, supplier);
    }
}
