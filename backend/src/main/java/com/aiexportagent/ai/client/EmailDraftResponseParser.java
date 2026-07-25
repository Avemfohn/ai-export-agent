package com.aiexportagent.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses a raw LLM text response into a structured {@link AiEmailDraftResult}.
 * Shared by {@link OpenAiClient} and {@link AnthropicClient}.
 */
final class EmailDraftResponseParser {

    private EmailDraftResponseParser() {
    }

    static AiEmailDraftResult parse(String rawText, String provider, String model, ObjectMapper objectMapper) {
        String json = JsonExtraction.extractJsonObject(rawText, provider);
        try {
            JsonNode node = objectMapper.readTree(json);
            String subject = node.path("subject").asText("");
            String body = node.path("body").asText("");
            return new AiEmailDraftResult(subject, body, provider, model);
        } catch (AiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new AiClientException(
                    "Could not parse " + provider + " response as structured JSON: " + rawText, e);
        }
    }
}
