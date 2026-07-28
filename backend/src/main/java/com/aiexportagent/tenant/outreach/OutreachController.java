package com.aiexportagent.tenant.outreach;

import com.aiexportagent.tenant.outreach.dto.OutreachEmailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * tenantId is never accepted from the client — OutreachEmailService pulls it
 * from TenantContext. The {@code id} path variable is likewise never trusted:
 * it's resolved through a tenant-scoped lookup, so another tenant's id 404s.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/outreach-emails")
public class OutreachController {

    private final OutreachEmailService outreachEmailService;

    @GetMapping
    public List<OutreachEmailResponse> list() {
        return outreachEmailService.listForCurrentTenant().stream()
                .map(OutreachEmailResponse::from)
                .toList();
    }

    /**
     * Puts a FAILED email back on the send queue. Narrowly scoped recovery
     * action, not a general status editor — 409 if the email isn't FAILED.
     */
    @PostMapping("/{id}/requeue")
    public OutreachEmailResponse requeue(@PathVariable UUID id) {
        return OutreachEmailResponse.from(outreachEmailService.requeueForCurrentTenant(id));
    }
}
