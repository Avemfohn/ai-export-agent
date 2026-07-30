package com.aiexportagent.ai.client;

import java.util.List;

/**
 * The placeholder tokens an email draft template may use.
 *
 * <p>This set is deliberately small and frozen. Every token is a promise the
 * product has to keep across all three {@link AiClient} implementations, and
 * the mock client substitutes them by exact literal match — so adding one here
 * without wiring it into {@code MockAiClient.substitutePlaceholders} would ship
 * a token that silently survives into a real customer email.
 *
 * <p>Mirrored on the frontend in
 * {@code frontend/lib/email-template-placeholders.ts}, which powers the
 * settings-page preview and its unknown-token warning. There is no shared
 * schema mechanism between the two, so the cross-reference comment is the
 * honest ceiling — change both together.
 */
public final class EmailTemplatePlaceholders {

    public static final String COMPANY_NAME = "{{companyName}}";
    public static final String CONTACT_FIRST_NAME = "{{contactFirstName}}";
    public static final String SENDER_NAME = "{{senderName}}";
    public static final String SECTOR = "{{sector}}";

    public static final List<String> ALL =
            List.of(COMPANY_NAME, CONTACT_FIRST_NAME, SENDER_NAME, SECTOR);

    private EmailTemplatePlaceholders() {
    }
}
