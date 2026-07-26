package com.aiexportagent.tenant.outreach;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "outreach_emails")
public class OutreachEmail extends Auditable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_lead_id", nullable = false)
    private UUID tenantLeadId;

    @Column(name = "to_email", length = 320, nullable = false)
    private String toEmail;

    @Column(name = "subject", length = 500, nullable = false)
    private String subject;

    @Column(name = "body", columnDefinition = "text", nullable = false)
    private String body;

    /** CHECK constraint values: DRAFT, QUEUED, SENT, FAILED, BOUNCED. */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    /** Populated only when status transitions to FAILED. */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
