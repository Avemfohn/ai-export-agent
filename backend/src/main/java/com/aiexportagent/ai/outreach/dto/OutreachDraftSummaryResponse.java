package com.aiexportagent.ai.outreach.dto;

/**
 * {@code skippedNoTemplate} counts leads whose resolved email template has no
 * usable subject/body. Those are skipped <em>before</em> the AI call rather
 * than drafted, because the drafting client turns a missing subject/body into
 * an empty string — which the automated pipeline would then send as a blank
 * email. Like {@code skippedNoContact} this is a countable non-error: the lead
 * is re-evaluated on the next tick, and costs nothing until the template is
 * fixed.
 *
 * <p>{@code skippedCampaignNotActive} counts leads parked in a campaign that
 * isn't ACTIVE. These are counted rather than filtered out of the candidate
 * set on purpose: if they were filtered, {@code leadsEvaluated} would drop and
 * "no approved leads" would be indistinguishable from "twelve leads sitting in
 * a paused campaign".
 *
 * <p>{@code skippedLowConfidenceContact} counts leads whose only contacts score
 * below {@code app.outreach.min-contact-confidence} — currently freemail
 * addresses imported from a trade-fair list. Kept separate from
 * {@code skippedNoContact} because that counter means "no contact on file", and
 * reporting it here would be a lie: there <em>is</em> a contact, it is just not
 * trusted enough to email automatically. Conflating the two would hide the one
 * signal that explains why a supplier with a visible email never gets contacted.
 */
public record OutreachDraftSummaryResponse(
        int leadsEvaluated,
        int drafted,
        int skippedNoContact,
        int skippedNoTemplate,
        int skippedCampaignNotActive,
        int skippedLowConfidenceContact,
        int failed
) {
}
