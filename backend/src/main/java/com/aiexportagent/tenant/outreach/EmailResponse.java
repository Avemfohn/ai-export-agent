package com.aiexportagent.tenant.outreach;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "email_responses")
public class EmailResponse extends Auditable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "outreach_email_id", nullable = false)
    private UUID outreachEmailId;

    @Column(name = "from_email", length = 320)
    private String fromEmail;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    /** CHECK constraint values: INTERESTED, NOT_INTERESTED, NEEDS_INFO, OUT_OF_OFFICE, UNSUBSCRIBE, SPAM, UNKNOWN. */
    @Column(name = "classified_intent", length = 30)
    private String classifiedIntent;

    // Simple String mapping for the JSONB column for this skeleton — no JSON<->POJO wiring yet.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classification_metadata", columnDefinition = "jsonb", nullable = false)
    private String classificationMetadata;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;
}
