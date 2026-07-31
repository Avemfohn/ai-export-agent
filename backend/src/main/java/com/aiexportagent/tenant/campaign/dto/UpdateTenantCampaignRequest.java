package com.aiexportagent.tenant.campaign.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Full replacement of a campaign's editable content (PUT, not PATCH).
 *
 * <p>PATCH's "null means leave unchanged" convention — used by
 * {@code UpdateTenantSettingsRequest} — relies on every writable column being
 * NOT NULL, so that "clear to null" is never a legal operation and the
 * absent-vs-explicit-null ambiguity has no observable effect. That does not
 * hold here: {@code description} is nullable, so PATCH genuinely could not
 * distinguish "leave the description alone" from "clear it". Full replacement
 * has no "unchanged" concept at all, which dissolves the ambiguity without a
 * migration or a second convention.
 *
 * <p>One carve-out: a null {@code emailDraftTemplate} leaves the existing
 * snapshot in place rather than clearing it. That column is NOT NULL, so
 * "replace it with nothing" isn't expressible — and blanking it would make
 * every lead in the campaign fall back to the tenant default, which is a
 * surprising thing for an omitted field to do.
 *
 * <p>{@code status} is deliberately excluded — it has its own narrow endpoint
 * with transition rules, the same split {@code auto_approve_threshold} and
 * {@code PATCH /api/leads/{id}/status} already use. Folding it in here would
 * also mean a one-click Pause had to send the whole form, clobbering any
 * concurrent edit.
 */
public record UpdateTenantCampaignRequest(
        String name,
        String description,
        JsonNode emailDraftTemplate
) {
}
