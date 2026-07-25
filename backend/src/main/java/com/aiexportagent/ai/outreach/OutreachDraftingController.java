package com.aiexportagent.ai.outreach;

import com.aiexportagent.ai.outreach.dto.OutreachDraftSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * tenantId is never accepted from the client — OutreachDraftingService pulls
 * it from TenantContext (via TenantSettingsService/TenantLeadService/OutreachEmailService).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/outreach-emails")
public class OutreachDraftingController {

    private final OutreachDraftingService outreachDraftingService;

    @PostMapping("/draft")
    public OutreachDraftSummaryResponse draft() {
        return outreachDraftingService.draftForCurrentTenant();
    }
}
