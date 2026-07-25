package com.aiexportagent.ai.client;

/**
 * Provider-agnostic LLM scoring abstraction. Exactly one implementation is
 * active at a time, selected by {@code app.ai.provider}
 * ({@link MockAiClient}, {@link OpenAiClient}, or {@link AnthropicClient}).
 */
public interface AiClient {

    /**
     * @throws AiClientException if the call fails or the response can't be
     *         parsed into a structured result.
     */
    AiScoringResult score(AiScoringRequest request);
}
