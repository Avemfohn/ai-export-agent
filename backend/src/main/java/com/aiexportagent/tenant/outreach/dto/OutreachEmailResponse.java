package com.aiexportagent.tenant.outreach.dto;

import com.aiexportagent.tenant.outreach.OutreachEmail;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutreachEmailResponse(
        UUID id,
        UUID tenantLeadId,
        String toEmail,
        String subject,
        String body,
        String status,
        String providerMessageId,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static OutreachEmailResponse from(OutreachEmail email) {
        return new OutreachEmailResponse(
                email.getId(),
                email.getTenantLeadId(),
                email.getToEmail(),
                email.getSubject(),
                email.getBody(),
                email.getStatus(),
                email.getProviderMessageId(),
                email.getSentAt(),
                email.getCreatedAt(),
                email.getUpdatedAt()
        );
    }
}
