package com.aiexportagent.email.scheduling;

import com.aiexportagent.common.tenant.TenantContext;
import com.aiexportagent.email.EmailSendRequest;
import com.aiexportagent.email.EmailSendResult;
import com.aiexportagent.email.EmailSender;
import com.aiexportagent.tenant.lead.TenantLeadService;
import com.aiexportagent.tenant.outreach.OutreachEmail;
import com.aiexportagent.tenant.outreach.OutreachEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Second half of the automated approve -> draft -> send pipeline: on a fixed
 * delay, sends the globally oldest {@code send-batch-size} QUEUED emails
 * (across all tenants — the conservative pacing knob from CLAUDE.md's
 * automated outreach pipeline; default 1 email/60s). On success marks the
 * email SENT and cascades the lead to EMAIL_SENT; on failure marks the email
 * FAILED with the error message and leaves the lead APPROVED — no retry (see
 * CLAUDE.md: a misconfigured integration must not silently hammer itself).
 *
 * <p>Same {@link TenantContext} discipline as {@code OutreachQueueingScheduler}:
 * set to the email's own tenant before any tenant-scoped call, always
 * cleared in a {@code finally}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutreachSendingScheduler {

    private final OutreachEmailService outreachEmailService;
    private final TenantLeadService tenantLeadService;
    private final EmailSender emailSender;

    @Value("${app.email.send-batch-size:1}")
    private int sendBatchSize;

    /**
     * Processes the batch <strong>serially</strong>, and that is load-bearing:
     * it's the only thing preventing a row from being sent twice, since
     * {@code markFailed}/{@code markSent} commit only after the attempt is
     * fully over, and {@code OutreachEmail} has no {@code @Version} column to
     * fall back on. A requeued email (see
     * {@code OutreachEmailService.requeueForCurrentTenant}) re-enters this
     * loop as an ordinary QUEUED row. If this is ever parallelised or made
     * {@code @Async} per email, add a status guard on the UPDATE first.
     */
    @Scheduled(fixedDelayString = "${app.email.send-interval-ms:60000}")
    public void sendQueuedEmails() {
        List<OutreachEmail> batch = outreachEmailService.findOldestQueuedGlobal(sendBatchSize);
        for (OutreachEmail email : batch) {
            TenantContext.set(email.getTenantId());
            try {
                EmailSendResult result = emailSender.send(
                        new EmailSendRequest(email.getToEmail(), email.getSubject(), email.getBody()));
                outreachEmailService.markSent(email.getId(), result.providerMessageId());
                tenantLeadService.markEmailSentForCurrentTenant(email.getTenantLeadId());
                log.info("Sent outreach email {} for tenant {}", email.getId(), email.getTenantId());
            } catch (Exception e) {
                // Caught broadly and never retried — see class Javadoc.
                outreachEmailService.markFailed(email.getId(), e.getMessage());
                log.warn("Failed to send outreach email {} for tenant {}: {}",
                        email.getId(), email.getTenantId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
