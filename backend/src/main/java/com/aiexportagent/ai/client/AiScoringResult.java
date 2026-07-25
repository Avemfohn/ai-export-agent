package com.aiexportagent.ai.client;

/**
 * Structured output of a scoring call. {@code score} is 0-100. {@code model}
 * is {@code null} for the mock provider.
 */
public record AiScoringResult(
        int score,
        String rationale,
        String provider,
        String model
) {
}
