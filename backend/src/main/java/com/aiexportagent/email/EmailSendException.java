package com.aiexportagent.email;

/**
 * Wraps an email-provider send failure. Callers (see
 * {@code OutreachSendingScheduler}) catch this per-email and mark it FAILED
 * rather than aborting the whole batch or retrying.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
