package com.aiexportagent.common.validation;

import com.aiexportagent.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;

/**
 * Write-boundary validation for the free-form JSONB settings columns
 * (buyer_criteria, target_sectors, target_regions, email_draft_template) and
 * the identically-shaped tenant_campaigns snapshot columns.
 *
 * <p>Deliberately stateless and Spring-free so it can be unit-tested without a
 * context, and callable from any owning service (tenant.account today,
 * tenant.campaign in the next phase) without duplicating the rules.
 *
 * <p><strong>Why this exists.</strong> Two silent failure modes make write-time
 * validation the only line of defence:
 * <ul>
 *   <li>{@code LeadScoringService} builds the scoring prompt from these columns,
 *       so a non-object criteria value corrupts every prompt.</li>
 *   <li>{@code MockAiClient.draftEmail} reads {@code path("subject").asText("")},
 *       so a template missing subject/body yields an <em>empty</em> email that
 *       the automated pipeline will happily send.</li>
 * </ul>
 *
 * <p>Validation is about <em>shape</em>, never about the contents of
 * buyer_criteria — that stays opaque so the LLM interprets it holistically
 * (see CLAUDE.md). The one exception is the email template's subject/body,
 * whose presence is a hard requirement of the drafting code.
 *
 * <p>Rejects rather than silently trimming or de-duplicating: silent cleaning
 * makes the stored value disagree with what the user sees on screen.
 */
public final class SettingsJsonValidator {

    /** Goes verbatim into an LLM prompt — an oversized blob is a billing incident, not a UX problem. */
    private static final int MAX_CRITERIA_CHARS = 8000;
    private static final int MAX_LIST_ITEMS = 50;
    private static final int MAX_LIST_ITEM_CHARS = 200;
    private static final int MAX_SUBJECT_CHARS = 300;
    private static final int MAX_BODY_CHARS = 10000;
    private static final int MAX_NOTES_CHARS = 4000;

    private SettingsJsonValidator() {
    }

    /** Buyer criteria: any JSON object. Contents intentionally unconstrained. */
    public static void validateBuyerCriteria(JsonNode node, String label) {
        requireObject(node, label);
        if (node.toString().length() > MAX_CRITERIA_CHARS) {
            throw badRequest(label + " is too large (max " + MAX_CRITERIA_CHARS + " characters)");
        }
    }

    /** Target sectors / regions: a JSON array of non-blank strings. */
    public static void validateStringArray(JsonNode node, String label) {
        if (node == null || !node.isArray()) {
            throw badRequest(label + " must be a JSON array");
        }
        if (node.size() > MAX_LIST_ITEMS) {
            throw badRequest(label + " supports at most " + MAX_LIST_ITEMS + " entries");
        }
        for (JsonNode item : node) {
            if (!item.isTextual() || item.asText().isBlank()) {
                throw badRequest(label + " must contain only non-empty text values");
            }
            if (item.asText().length() > MAX_LIST_ITEM_CHARS) {
                throw badRequest(label + " entries must be " + MAX_LIST_ITEM_CHARS + " characters or fewer");
            }
        }
    }

    /**
     * Email template: a JSON object with a non-blank subject and body. Unknown
     * keys are allowed and preserved — only the two fields the drafting code
     * actually depends on are required.
     */
    public static void validateEmailDraftTemplate(JsonNode node, String label) {
        requireObject(node, label);
        requireText(node, "subject", label + " subject", MAX_SUBJECT_CHARS, true);
        requireText(node, "body", label + " body", MAX_BODY_CHARS, true);
        requireText(node, "notes", label + " notes", MAX_NOTES_CHARS, false);
    }

    private static void requireObject(JsonNode node, String label) {
        if (node == null || !node.isObject()) {
            throw badRequest(label + " must be a JSON object");
        }
    }

    private static void requireText(JsonNode parent, String field, String label, int maxChars, boolean required) {
        JsonNode value = parent.get(field);
        if (value == null || value.isNull()) {
            if (required) {
                throw badRequest(label + " is required");
            }
            return;
        }
        if (!value.isTextual()) {
            throw badRequest(label + " must be text");
        }
        if (required && value.asText().isBlank()) {
            throw badRequest(label + " is required");
        }
        if (value.asText().length() > maxChars) {
            throw badRequest(label + " must be " + maxChars + " characters or fewer");
        }
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
