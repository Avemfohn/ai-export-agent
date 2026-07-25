package com.aiexportagent.tenant.outreach;

import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.tenant.lead.TenantLeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        tenantLeadService.assertOwnedByCurrentTenant(tenantLeadId);

        OutreachEmail email = new OutreachEmail();
        email.setTenantId(TenantContext.get());
        email.setTenantLeadId(tenantLeadId);
        email.setToEmail(toEmail);
        email.setSubject(subject);
        email.setBody(body);
        email.setStatus("DRAFT");
        return outreachEmailRepository.save(email);
    }
}
