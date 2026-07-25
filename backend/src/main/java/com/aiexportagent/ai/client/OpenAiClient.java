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
 * {@link ScoringResponseParser}/{@link EmailDraftResponseParser} defensively
 * — models don't always comply perfectly). Active only when
 * {@code app.ai.provider=openai}; requires {@code app.ai.openai.api-key}
 * (env {@code OPENAI_API_KEY}) to be set.
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
        String content = complete(PromptBuilder.scoringSystemPrompt(), PromptBuilder.scoringUserPrompt(request));
        return ScoringResponseParser.parse(content, "openai", model, objectMapper);
    }

    @Override
    public AiEmailDraftResult draftEmail(AiEmailDraftRequest request) {
        String content = complete(PromptBuilder.draftingSystemPrompt(), PromptBuilder.draftingUserPrompt(request));
        return EmailDraftResponseParser.parse(content, "openai", model, objectMapper);
    }

    /** @throws AiClientException if the HTTP call itself fails. */
    private String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return response.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new AiClientException("OpenAI call failed: " + e.getMessage(), e);
        }
    }
}
