package com.aiexportagent.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Deterministic stand-in for a real email provider — no HTTP call, no cost,
 * no API key required. Active by default ({@code app.email.provider=mock}
 * or unset), same role as {@code MockAiClient} for the AI abstraction: proves
 * the queue → send → mark-sent plumbing end-to-end for free.
 *
 * <p>Always succeeds, except for the reserved local-part {@code failtest@}
 * (e.g. {@code failtest@example.com}), which deterministically throws — a
 * manual escape hatch for exercising the FAILED path without needing real
 * Mailgun credentials.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailSender implements EmailSender {

    @Override
    public EmailSendResult send(EmailSendRequest request) {
        if (request.toEmail() != null && request.toEmail().startsWith("failtest@")) {
            throw new EmailSendException("Mock forced failure for " + request.toEmail());
        }
        log.info("Mock-sending email to {} — subject: \"{}\"", request.toEmail(), request.subject());
        return new EmailSendResult("mock-" + UUID.randomUUID(), "mock");
    }
}
