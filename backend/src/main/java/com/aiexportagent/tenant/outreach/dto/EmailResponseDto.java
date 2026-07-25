package com.aiexportagent.tenant.outreach.dto;

import com.aiexportagent.tenant.outreach.EmailResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmailResponseDto(
        UUID id,
        UUID outreachEmailId,
        String fromEmail,
        String subject,
        String body,
        String classifiedIntent,
        OffsetDateTime receivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static EmailResponseDto from(EmailResponse response) {
        return new EmailResponseDto(
                response.getId(),
                response.getOutreachEmailId(),
                response.getFromEmail(),
                response.getSubject(),
                response.getBody(),
                response.getClassifiedIntent(),
                response.getReceivedAt(),
                response.getCreatedAt(),
                response.getUpdatedAt()
        );
    }
}
