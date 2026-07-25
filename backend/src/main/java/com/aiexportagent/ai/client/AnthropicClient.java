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
 * Real Anthropic Messages API client. Anthropic has no native "JSON mode"
 * equivalent to OpenAI's {@code response_format} — structured output relies
 * entirely on the prompt's instructions plus {@link ScoringResponseParser}/
 * {@link EmailDraftResponseParser}'s lenient extraction (this is exactly the
 * gap CLAUDE.md flags as LangChain4j's weak point too; a raw client has the
 * same constraint, it's just handled explicitly here instead of hidden
 * behind a library). Active only when {@code app.ai.provider=anthropic};
 * requires {@code app.ai.anthropic.api-key} (env {@code ANTHROPIC_API_KEY}).
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "anthropic")
public class AnthropicClient implements AiClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public AnthropicClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.ai.anthropic.api-key}") String apiKey,
            @Value("${app.ai.anthropic.model}") String model) {
        this.restClient = builder
                .baseUrl("https://api.anthropic.com/v1")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public AiScoringResult score(AiScoringRequest request) {
        String content = complete(PromptBuilder.scoringSystemPrompt(), PromptBuilder.scoringUserPrompt(request));
        return ScoringResponseParser.parse(content, "anthropic", model, objectMapper);
    }

    @Override
    public AiEmailDraftResult draftEmail(AiEmailDraftRequest request) {
        String content = complete(PromptBuilder.draftingSystemPrompt(), PromptBuilder.draftingUserPrompt(request));
        return EmailDraftResponseParser.parse(content, "anthropic", model, objectMapper);
    }

    /** @throws AiClientException if the HTTP call itself fails. */
    private String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 512,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/messages")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return response.path("content").path(0).path("text").asText();
        } catch (Exception e) {
            throw new AiClientException("Anthropic call failed: " + e.getMessage(), e);
        }
    }
}
