package com.aiexportagent.email;

/**
 * Provider-agnostic outbound-email abstraction. Exactly one implementation
 * is active at a time, selected by {@code app.email.provider}
 * ({@link MockEmailSender} or {@link MailgunEmailSender}) — same
 * one-interface-many-providers shape as {@code AiClient}.
 */
public interface EmailSender {

    /**
     * @throws EmailSendException if the send fails. Callers must not retry —
     *         see {@code OutreachSendingScheduler}.
     */
    EmailSendResult send(EmailSendRequest request);
}
