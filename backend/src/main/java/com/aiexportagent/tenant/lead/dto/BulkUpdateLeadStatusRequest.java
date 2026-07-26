package com.aiexportagent.tenant.lead.dto;

import java.util.List;
import java.util.UUID;

public record BulkUpdateLeadStatusRequest(List<UUID> leadIds, String status) {
}
