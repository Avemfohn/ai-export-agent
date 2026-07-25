package com.aiexportagent.tenant.scraping;

import com.aiexportagent.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapingJobService {

    private final ScrapingJobRepository scrapingJobRepository;

    public List<ScrapingJob> listForCurrentTenant() {
        return scrapingJobRepository.findByTenantId(TenantContext.get());
    }
}
