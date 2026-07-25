package com.aiexportagent.tenant.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * tenantId is never accepted from the client — NotificationService pulls it
 * from TenantContext.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> list() {
        return notificationService.listForCurrentTenant();
    }
}
