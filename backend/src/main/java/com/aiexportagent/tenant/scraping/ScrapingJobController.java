package com.aiexportagent.tenant.scraping;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * tenantId is never accepted from the client — ScrapingJobService pulls it
 * from TenantContext.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraping-jobs")
public class ScrapingJobController {

    private final ScrapingJobService scrapingJobService;

    @GetMapping
    public List<ScrapingJob> list() {
        return scrapingJobService.listForCurrentTenant();
    }
}
