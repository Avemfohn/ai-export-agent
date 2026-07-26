package com.aiexportagent.tenant.outreach;

import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.tenant.lead.TenantLeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutreachEmailService {

    private final OutreachEmailRepository outreachEmailRepository;
    private final TenantLeadService tenantLeadService;

    public List<OutreachEmail> listForCurrentTenant() {
        return outreachEmailRepository.findByTenantId(TenantContext.get());
    }

    /** Used by the AI outreach-drafting engine to skip tenant_leads already drafted for. */
    public Set<UUID> getLeadIdsWithOutreachForCurrentTenant() {
        return outreachEmailRepository.findByTenantId(TenantContext.get()).stream()
                .map(OutreachEmail::getTenantLeadId)
                .collect(Collectors.toSet());
    }

    /**
     * Creates a new DRAFT outreach_email for the current tenant. tenantId is
     * always taken from {@link TenantContext}, never a parameter — same rule
     * as every other write path in this codebase. REQUIRES_NEW: always its
     * own short transaction, committed immediately — the AI drafting call
     * that produced subject/body already happened outside any transaction.
     *
     * <p>{@code tenantLeadId} is re-validated against the current tenant
     * before insert (defense-in-depth — see
     * {@link TenantLeadService#assertOwnedByCurrentTenant}) rather than
     * trusted as already tenant-safe just because today's only caller
     * happens to source it from a tenant-filtered query.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OutreachEmail createDraftEmail(UUID tenantLeadId, String toEmail, String subject, String body) {
        return createEmail(tenantLeadId, toEmail, subject, body, "DRAFT");
    }

    /**
     * Same as {@link #createDraftEmail} but inserts directly as QUEUED — used
     * by the automated {@code OutreachQueueingScheduler} path, where DRAFT is
     * deliberately never a persisted state (see CLAUDE.md automated outreach
     * pipeline: "DRAFT is invisible in the automated flow").
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OutreachEmail createQueuedEmail(UUID tenantLeadId, String toEmail, String subject, String body) {
        return createEmail(tenantLeadId, toEmail, subject, body, "QUEUED");
    }

    private OutreachEmail createEmail(UUID tenantLeadId, String toEmail, String subject, String body, String status) {
        tenantLeadService.assertOwnedByCurrentTenant(tenantLeadId);

        OutreachEmail email = new OutreachEmail();
        email.setTenantId(TenantContext.get());
        email.setTenantLeadId(tenantLeadId);
        email.setToEmail(toEmail);
        email.setSubject(subject);
        email.setBody(body);
        email.setStatus(status);
        return outreachEmailRepository.save(email);
    }

    /**
     * Deliberately NOT tenant-scoped — see
     * {@link OutreachEmailRepository#findByStatusOrderByCreatedAtAsc}. Used
     * only by {@code OutreachSendingScheduler} to pick the next batch to
     * send, oldest-queued-first, across all tenants.
     */
    public List<OutreachEmail> findOldestQueuedGlobal(int limit) {
        return outreachEmailRepository.findByStatusOrderByCreatedAtAsc("QUEUED", PageRequest.of(0, limit));
    }

    /**
     * Marks a QUEUED email SENT after a successful provider call. Callers
     * must set {@link TenantContext} to this email's own tenant first (see
     * {@code OutreachSendingScheduler}) — verified here as a defense-in-depth
     * invariant check, not trusted blindly, since this method runs outside
     * any HTTP request where that discipline would normally be enforced by
     * {@code TenantContextFilter}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID emailId, String providerMessageId) {
        OutreachEmail email = getOwnedByCurrentTenant(emailId);
        email.setStatus("SENT");
        email.setProviderMessageId(providerMessageId);
        email.setSentAt(OffsetDateTime.now());
        outreachEmailRepository.save(email);
    }

    /** Same tenant-ownership discipline as {@link #markSent}. No retry — see CLAUDE.md. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID emailId, String errorMessage) {
        OutreachEmail email = getOwnedByCurrentTenant(emailId);
        email.setStatus("FAILED");
        email.setErrorMessage(errorMessage);
        outreachEmailRepository.save(email);
    }

    private OutreachEmail getOwnedByCurrentTenant(UUID emailId) {
        OutreachEmail email = outreachEmailRepository.findById(emailId)
                .orElseThrow(() -> new NotFoundException("Outreach email not found: " + emailId));
        if (!email.getTenantId().equals(TenantContext.get())) {
            throw new IllegalStateException(
                    "TenantContext/email tenant mismatch for outreach email " + emailId
                            + " — this indicates a scheduler bug, not a client-facing error.");
        }
        return email;
    }
}
