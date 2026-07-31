package com.aiexportagent.tenant.campaign;

import java.util.Set;

/**
 * Mirrors the CHECK constraint on tenant_campaigns.status in
 * V1__initial_schema.sql exactly.
 *
 * <p>Unlike most status enums here, this one carries behaviour:
 * {@link #ACTIVE} is the only value whose leads are eligible for outreach
 * (see {@code OutreachDraftingService}). Pausing or archiving a campaign
 * genuinely stops its leads being drafted and queued.
 */
public enum CampaignStatus {

    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED;

    /** The only status whose leads get drafted and queued. */
    public boolean allowsOutreach() {
        return this == ACTIVE;
    }

    /**
     * Legal transitions. Deliberately not free-form: without this, a
     * COMPLETED campaign could silently resume sending and ARCHIVED would
     * mean nothing.
     *
     * <p>{@code ARCHIVED -> DRAFT} exists on purpose — a terminal ARCHIVED
     * would make a mis-archive recoverable only by hand-editing the database,
     * which is exactly the dead end Phase 0 removed for failed sends. Coming
     * back as DRAFT preserves "archived campaigns don't send" while keeping
     * recovery inside the product.
     *
     * <p>Staying put is always allowed, so re-issuing the same status is an
     * idempotent no-op rather than a 409.
     */
    public boolean canTransitionTo(CampaignStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case DRAFT -> Set.of(ACTIVE, ARCHIVED).contains(target);
            case ACTIVE -> Set.of(PAUSED, COMPLETED, ARCHIVED).contains(target);
            case PAUSED -> Set.of(ACTIVE, COMPLETED, ARCHIVED).contains(target);
            case COMPLETED -> target == ARCHIVED;
            case ARCHIVED -> target == DRAFT;
        };
    }
}
