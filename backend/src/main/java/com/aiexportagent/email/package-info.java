/**
 * Provider-agnostic outbound email-sending abstraction ({@link
 * com.aiexportagent.email.EmailSender}, {@link
 * com.aiexportagent.email.MockEmailSender}, {@link
 * com.aiexportagent.email.MailgunEmailSender}) used by {@code
 * OutreachSendingScheduler} to actually deliver queued {@code
 * outreach_emails} rows. The inbound-reply-webhook side (classifying replies
 * into {@code email_responses}) is not built yet — see CLAUDE.md.
 */
package com.aiexportagent.email;
