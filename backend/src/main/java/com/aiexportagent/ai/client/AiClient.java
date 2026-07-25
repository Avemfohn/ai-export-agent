package com.aiexportagent.ai.client;

/**
 * Provider-agnostic LLM abstraction covering both AI operations this app
 * uses: lead-scoring and outreach-email-draft customization. Exactly one
 * implementation is active at a time, selected by {@code app.ai.provider}
 * ({@link MockAiClient}, {@link OpenAiClient}, or {@link AnthropicClient}) —
 * one provider choice covers both operations.
 */
public interface AiClient {

    /**
     * @throws AiClientException if the call fails or the response can't be
     *         parsed into a structured result.
     */
    AiScoringResult score(AiScoringRequest request);

    /**
     * @throws AiClientException if the call fails or the response can't be
     *         parsed into a structured result.
     */
    AiEmailDraftResult draftEmail(AiEmailDraftRequest request);
}
