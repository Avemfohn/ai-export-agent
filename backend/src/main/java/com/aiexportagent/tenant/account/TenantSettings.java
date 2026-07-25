package com.aiexportagent.tenant.account;

import com.aiexportagent.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenant_settings")
public class TenantSettings extends Auditable {

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    // Simple String mapping for JSONB columns for this skeleton — no JSON<->POJO wiring yet.
    @Column(name = "buyer_criteria", columnDefinition = "jsonb", nullable = false)
    private String buyerCriteria;

    @Column(name = "target_sectors", columnDefinition = "jsonb", nullable = false)
    private String targetSectors;

    @Column(name = "target_regions", columnDefinition = "jsonb", nullable = false)
    private String targetRegions;

    @Column(name = "email_sender_name", length = 255)
    private String emailSenderName;

    @Column(name = "email_sender_address", length = 320)
    private String emailSenderAddress;

    @Column(name = "whatsapp_notify_number", length = 50)
    private String whatsappNotifyNumber;

    @Column(name = "notification_prefs", columnDefinition = "jsonb", nullable = false)
    private String notificationPrefs;
}
