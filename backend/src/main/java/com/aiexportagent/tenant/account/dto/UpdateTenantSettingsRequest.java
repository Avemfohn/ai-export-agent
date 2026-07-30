package com.aiexportagent.tenant.account.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Partial update of the current tenant's settings: a {@code null} (or absent)
 * field means "leave unchanged".
 *
 * <p>That ambiguity is harmless here specifically because every column this
 * record can write is {@code NOT NULL} — "clear to null" isn't a legal
 * operation for any of them, so absent-vs-explicit-null has no observable
 * difference. {@code auto_approve_threshold} is the one settings field where
 * {@code null} is meaningful ("off"), which is exactly why it keeps its own
 * dedicated endpoint rather than joining this one.
 *
 * <p>Fields are {@link JsonNode}, not {@code String}: malformed JSON is then
 * rejected by Jackson as {@code HttpMessageNotReadableException} (400 via
 * {@code GlobalExceptionHandler}) before any application code runs, and shape
 * checks reduce to {@code isObject()}/{@code isArray()}.
 *
 * <p><strong>{@code emailSenderName}/{@code emailSenderAddress} are deliberately
 * absent, and absent means unwritable.</strong> Spring Boot disables
 * {@code FAIL_ON_UNKNOWN_PROPERTIES}, so a client sending them is silently
 * ignored rather than binding. Those columns are coupled to SPF/DKIM and
 * sending-domain reputation and are configured during onboarding, not by the
 * customer — do not add them here "for later".
 *
 * <p>{@code whatsappNotifyNumber}/{@code notificationPrefs} are likewise
 * omitted until the phase that actually consumes them.
 */
public record UpdateTenantSettingsRequest(
        JsonNode buyerCriteria,
        JsonNode targetSectors,
        JsonNode targetRegions,
        JsonNode emailDraftTemplate
) {
}
