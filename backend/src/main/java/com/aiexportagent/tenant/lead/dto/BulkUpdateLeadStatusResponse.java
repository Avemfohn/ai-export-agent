package com.aiexportagent.tenant.lead.dto;

public record BulkUpdateLeadStatusResponse(
        int requested,
        int updated,
        int skippedWrongStatus,
        int notFoundOrForeignTenant
) {
}
