package com.aiexportagent.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Real OpenAI Chat Completions client, using {@code response_format:
 * json_object} to encourage structured output (still passed through
 * {@link ScoringResponseParser} defensively — models don't always comply
 * perfectly). Active only when {@code app.ai.provider=openai}; requires
 * {@code app.ai.openai.api-key} (env {@code OPENAI_API_KEY}) to be set.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiClient implements AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.ai.openai.api-key}") String apiKey,
            @Value("${app.ai.openai.model}") String model) {
        this.restClient = builder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public AiScoringResult score(AiScoringRequest request) {
        Map<String, Object> body = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", ScoringPromptBuilder.systemPrompt()),
                        Map.of("role", "user", "content", ScoringPromptBuilder.userPrompt(request))
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("choices").path(0).path("message").path("content").asText();
            return ScoringResponseParser.parse(content, "openai", model, objectMapper);
        } catch (AiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new AiClientException("OpenAI scoring call failed: " + e.getMessage(), e);
        }
    }
}
