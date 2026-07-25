package com.aiexportagent.ai.client;

/**
 * Shared by {@link ScoringResponseParser} and {@link EmailDraftResponseParser}:
 * extracts the first {@code {...}} span from a raw LLM text response, tolerating
 * the model wrapping its JSON object in prose despite the prompt's instructions
 * — real-world models don't always emit JSON-only.
 */
final class JsonExtraction {

    private JsonExtraction() {
    }

    static String extractJsonObject(String rawText, String provider) {
        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new AiClientException("No JSON object found in " + provider + " response: " + rawText);
        }
        return rawText.substring(start, end + 1);
    }
}
