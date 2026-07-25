package com.aiexportagent.tenant.outreach;

import com.aiexportagent.tenant.outreach.dto.OutreachEmailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * tenantId is never accepted from the client — OutreachEmailService pulls it
 * from TenantContext.
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
}
