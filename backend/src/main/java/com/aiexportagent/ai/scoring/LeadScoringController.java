package com.aiexportagent.ai.scoring;

import com.aiexportagent.ai.scoring.dto.LeadScoringSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * tenantId is never accepted from the client — LeadScoringService pulls it
 * from TenantContext (via TenantSettingsService/TenantLeadService).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leads")
public class LeadScoringController {

    private final LeadScoringService leadScoringService;

    @PostMapping("/score")
    public LeadScoringSummaryResponse score() {
        return leadScoringService.scoreForCurrentTenant();
    }
}
