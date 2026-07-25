package com.aiexportagent.ai.client;

/**
 * Builds the system/user prompt text shared by {@link OpenAiClient} and
 * {@link AnthropicClient} — each provider wraps this text into its own
 * request shape. {@link MockAiClient} doesn't call an LLM and doesn't use
 * this. Covers both AI operations this client abstraction supports:
 * scoring ({@code scoring*}) and email-draft customization ({@code drafting*}).
 */
final class PromptBuilder {

    private PromptBuilder() {
    }

    static String scoringSystemPrompt() {
        return """
                You are a B2B buyer-qualification assistant for an export/manufacturing \
                company. Given a tenant's buyer criteria and a candidate company scraped \
                from the web, decide how well the candidate matches as a potential buyer.

                Respond with ONLY a single JSON object, no prose before or after it, in \
                exactly this shape:
                {"score": <integer 0-100>, "rationale": "<one or two sentence explanation>"}

                0 means clearly not a match, 100 means an excellent match. Base the score \
                strictly on the criteria and company details provided.""";
    }

    static String scoringUserPrompt(AiScoringRequest request) {
        return """
                Buyer criteria (JSON):
                %s

                Candidate company:
                - Name: %s
                - Domain: %s
                - Country: %s
                - Sector: %s
                - Description: %s"""
                .formatted(
                        request.buyerCriteriaJson(),
                        request.companyName(),
                        request.domain(),
                        nullToDash(request.country()),
                        nullToDash(request.sector()),
                        nullToDash(request.description())
                );
    }

    static String draftingSystemPrompt() {
        return """
                You are an outreach-email customization assistant for an export/manufacturing \
                company. You are given a client-authored base email template (with \
                {{placeholder}} tokens) and details about one specific recipient company and \
                contact. Your job is to CUSTOMIZE the template, not write a new email from \
                scratch: fill in the placeholders, and make light, grounded personalization \
                (e.g. referencing the recipient's real sector/description) — do not invent \
                facts, do not change the core pitch or any pricing claims, and follow any \
                instructions in the template's "notes" field exactly.

                Respond with ONLY a single JSON object, no prose before or after it, in \
                exactly this shape:
                {"subject": "<customized subject line>", "body": "<customized email body>"}""";
    }

    static String draftingUserPrompt(AiEmailDraftRequest request) {
        return """
                Base template (JSON, with subject/body/notes):
                %s

                Recipient company:
                - Name: %s
                - Domain: %s
                - Sector: %s
                - Description: %s

                Recipient contact:
                - Name: %s
                - Job title: %s

                Sender name to sign as: %s"""
                .formatted(
                        request.baseTemplateJson(),
                        request.companyName(),
                        request.domain(),
                        nullToDash(request.sector()),
                        nullToDash(request.description()),
                        nullToDash(request.contactFullName()),
                        nullToDash(request.contactJobTitle()),
                        nullToDash(request.senderName())
                );
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
