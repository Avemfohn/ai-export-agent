package com.aiexportagent.ai.client;

/** Structured output of a drafting call. {@code model} is {@code null} for the mock provider. */
public record AiEmailDraftResult(
        String subject,
        String body,
        String provider,
        String model
) {
}
