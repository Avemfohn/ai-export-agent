package com.aiexportagent.tenant.lead;

/**
 * Mirrors the CHECK constraint on tenant_leads.status in
 * V1__initial_schema.sql exactly.
 */
public enum LeadStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    EMAIL_SENT,
    NO_RESPONSE,
    INTERESTED,
    NOT_INTERESTED,
    BOUNCED,
    CONVERTED
}
