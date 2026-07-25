package com.aiexportagent.ai.client;

/**
 * Builds the system/user prompt text shared by {@link OpenAiClient} and
 * {@link AnthropicClient} — each provider wraps this text into its own
 * request shape. {@link MockAiClient} doesn't call an LLM and doesn't use
 * this.
 */
final class ScoringPromptBuilder {

    private ScoringPromptBuilder() {
    }

    static String systemPrompt() {
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

    static String userPrompt(AiScoringRequest request) {
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

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
