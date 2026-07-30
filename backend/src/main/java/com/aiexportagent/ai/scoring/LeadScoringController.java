package com.aiexportagent.ai.scoring;

import com.aiexportagent.ai.scoring.dto.CriteriaPreviewRequest;
import com.aiexportagent.ai.scoring.dto.CriteriaPreviewResponse;
import com.aiexportagent.ai.scoring.dto.LeadScoringSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * Try criteria against a small sample of the supplier pool without saving
     * them and without creating any leads. POST rather than GET because the
     * candidate criteria travel in the body.
     */
    @PostMapping("/score/preview")
    public CriteriaPreviewResponse previewCriteria(@RequestBody CriteriaPreviewRequest request) {
        return leadScoringService.previewCriteriaForCurrentTenant(request);
    }
}
