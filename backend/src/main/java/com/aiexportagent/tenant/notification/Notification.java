package com.aiexportagent.tenant.notification;

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
@Table(name = "notifications")
public class Notification extends Auditable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_user_id")
    private UUID tenantUserId;

    /** CHECK constraint values: WARM_REPLY, NEW_LEAD, SCRAPING_JOB_DONE, BOUNCE_ALERT, SYSTEM. */
    @Column(name = "type", length = 50, nullable = false)
    private String type;

    /** CHECK constraint values: DASHBOARD, WHATSAPP, EMAIL. */
    @Column(name = "channel", length = 30, nullable = false)
    private String channel;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;
}
