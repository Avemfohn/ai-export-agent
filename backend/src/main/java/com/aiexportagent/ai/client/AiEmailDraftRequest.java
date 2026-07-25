package com.aiexportagent.ai.client;

/**
 * Everything an {@link AiClient} needs to customize a client-authored base
 * draft template into a real subject/body for one supplier contact.
 * {@code baseTemplateJson} is the resolved template — the tenant campaign's
 * {@code email_draft_template_snapshot} if the lead belongs to one, else the
 * tenant's default {@code email_draft_template} — passed as raw JSON text,
 * same "stays opaque JSON" convention as {@link AiScoringRequest}.
 */
public record AiEmailDraftRequest(
        String baseTemplateJson,
        String companyName,
        String domain,
        String sector,
        String description,
        String contactFullName,
        String contactJobTitle,
        String senderName
) {
}
