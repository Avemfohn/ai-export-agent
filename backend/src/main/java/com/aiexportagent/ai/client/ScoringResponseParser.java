package com.aiexportagent.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses a raw LLM text response into a structured {@link AiScoringResult}.
 * Tolerates the model wrapping the JSON object in prose despite the prompt's
 * instructions by extracting the first {@code {...}} span before parsing —
 * real-world models don't always follow "JSON only" perfectly. Shared by
 * {@link OpenAiClient} and {@link AnthropicClient}.
 */
final class ScoringResponseParser {

    private ScoringResponseParser() {
    }

    static AiScoringResult parse(String rawText, String provider, String model, ObjectMapper objectMapper) {
        String json = extractJsonObject(rawText, provider);
        try {
            JsonNode node = objectMapper.readTree(json);
            int score = clamp(node.path("score").asInt(0));
            String rationale = node.path("rationale").asText("");
            return new AiScoringResult(score, rationale, provider, model);
        } catch (AiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new AiClientException(
                    "Could not parse " + provider + " response as structured JSON: " + rawText, e);
        }
    }

    private static String extractJsonObject(String rawText, String provider) {
        int start = rawText.indexOf('{');
        int end = rawText.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new AiClientException("No JSON object found in " + provider + " response: " + rawText);
        }
        return rawText.substring(start, end + 1);
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
