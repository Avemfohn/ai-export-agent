package com.aiexportagent.email;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Real Mailgun client, using their standard form-encoded {@code /messages}
 * send endpoint with HTTP Basic Auth (username {@code api}, password the API
 * key). Active only when {@code app.email.provider=mailgun}; requires
 * {@code app.email.mailgun.api-key} (env {@code MAILGUN_API_KEY}) and
 * {@code app.email.mailgun.domain} (env {@code MAILGUN_DOMAIN}) to be set.
 *
 * <p>The "From" address is a single global fallback ({@code
 * app.email.mailgun.from-address}, defaulting to {@code noreply@<domain>})
 * rather than each tenant's {@code tenant_settings.email_sender_address} —
 * wiring per-tenant sender identity into the actual envelope From header
 * (vs. today's use of {@code email_sender_name} only inside the drafted
 * body's signature placeholder) is out of scope for this pass and can follow
 * once a real Mailgun account/domain is actually being provisioned.
 */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mailgun")
public class MailgunEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String fromAddress;

    public MailgunEmailSender(
            RestClient.Builder builder,
            @Value("${app.email.mailgun.api-key}") String apiKey,
            @Value("${app.email.mailgun.domain}") String domain,
            @Value("${app.email.mailgun.from-address:}") String fromAddressPrefix) {
        this.restClient = builder
                .baseUrl("https://api.mailgun.net/v3/" + domain)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBasicAuth("api", apiKey);
                    return execution.execute(request, body);
                })
                .build();
        this.fromAddress = fromAddressPrefix.isBlank() ? "noreply@" + domain : fromAddressPrefix;
    }

    @Override
    public EmailSendResult send(EmailSendRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", fromAddress);
        form.add("to", request.toEmail());
        form.add("subject", request.subject());
        form.add("text", request.body());

        try {
            JsonNode response = restClient.post()
                    .uri("/messages")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            String messageId = response.path("id").asText(null);
            return new EmailSendResult(messageId, "mailgun");
        } catch (Exception e) {
            throw new EmailSendException("Mailgun send failed: " + e.getMessage(), e);
        }
    }
}
