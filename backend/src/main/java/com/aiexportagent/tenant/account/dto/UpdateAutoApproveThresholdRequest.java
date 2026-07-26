package com.aiexportagent.tenant.account.dto;

import java.math.BigDecimal;

/** Client sends {@code null} to turn auto-approve off. */
public record UpdateAutoApproveThresholdRequest(BigDecimal autoApproveThreshold) {
}
