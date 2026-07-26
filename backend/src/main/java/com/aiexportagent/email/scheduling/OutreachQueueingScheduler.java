package com.aiexportagent.email.scheduling;

import com.aiexportagent.ai.outreach.OutreachDraftingService;
import com.aiexportagent.ai.outreach.dto.OutreachDraftSummaryResponse;
import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.tenant.account.Tenant;
import com.aiexportagent.tenant.account.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * First half of the automated approve -> draft -> send pipeline: on a fixed
 * delay, loops every tenant that isn't SUSPENDED/CANCELLED and queues
 * (AI-drafts + inserts as QUEUED, never DRAFT — see
 * {@link OutreachDraftingService#queueForCurrentTenant}) every APPROVED lead
 * that doesn't have an outreach_email yet. TRIAL tenants are included
 * deliberately — a trial prospect needs the full automated flow to see the
 * product's value, not just ACTIVE (paying) ones.
 *
 * <p>This is the first code in this codebase that runs with no inbound HTTP
 * request, so unlike every other tenant-scoped call site, {@link
 * TenantContext} is NOT already populated by {@code TenantContextFilter} —
 * it must be set explicitly per tenant here, and cleared in a {@code
 * finally} block so a stale tenant id never leaks onto the next iteration
 * (Spring's default scheduler reuses one thread across ticks).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutreachQueueingScheduler {

    private static final Set<String> EXCLUDED_STATUSES = Set.of("SUSPENDED", "CANCELLED");

    private final TenantService tenantService;
    private final OutreachDraftingService outreachDraftingService;

    @Scheduled(fixedDelayString = "${app.email.queue-interval-ms:60000}")
    public void queueApprovedLeads() {
        List<Tenant> tenants = tenantService.listAll();
        for (Tenant tenant : tenants) {
            if (EXCLUDED_STATUSES.contains(tenant.getStatus())) {
                continue;
            }
            TenantContext.set(tenant.getId());
            try {
                OutreachDraftSummaryResponse summary = outreachDraftingService.queueForCurrentTenant();
                if (summary.leadsEvaluated() > 0) {
                    log.info("Outreach queueing for tenant {}: {}", tenant.getId(), summary);
                }
            } catch (Exception e) {
                // One tenant's failure must never block the others.
                log.warn("Outreach queueing failed for tenant {}: {}", tenant.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
