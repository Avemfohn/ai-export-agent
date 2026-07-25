package com.aiexportagent.ai.client;

/**
 * Wraps an AI provider call/parse failure. Callers (see
 * {@code LeadScoringService}) catch this per-supplier and skip rather than
 * aborting the whole batch.
 */
public class AiClientException extends RuntimeException {

    public AiClientException(String message) {
        super(message);
    }

    public AiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
