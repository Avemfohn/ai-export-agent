package com.aiexportagent.tenant.scraping;

import com.aiexportagent.common.exception.NotFoundException;
import com.aiexportagent.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapingJobService {

    private final ScrapingJobRepository scrapingJobRepository;

    public List<ScrapingJob> listForCurrentTenant() {
        return scrapingJobRepository.findByTenantId(TenantContext.get());
    }

    /**
     * Open a job for the current tenant and commit it immediately as RUNNING.
     *
     * <p><strong>The commit is the point.</strong> Suppliers created by an
     * upload carry {@code source_scraping_job_id}, so the job row has to exist
     * and be visible before the first chunk of suppliers is inserted. Writing
     * the job at the end with final counts — which would otherwise be simpler —
     * is impossible for that reason.
     *
     * <p>It also means a crash mid-import leaves an observable RUNNING row
     * rather than no trace at all, and it gives the PENDING/RUNNING/COMPLETED
     * states an actual use, which is the "proper state machine" this table was
     * designed for.
     *
     * <p>{@code tenantId} comes from {@link TenantContext}, never from client
     * input.
     */
    @Transactional
    public ScrapingJob startUploadJob(String paramsJson) {
        ScrapingJob job = new ScrapingJob();
        job.setTenantId(TenantContext.get());
        job.setSource("TRADE_FAIR_UPLOAD");
        job.setStatus("RUNNING");
        job.setParams(paramsJson);
        job.setResultSummary("{}");
        job.setCompaniesFound(0);
        job.setStartedAt(OffsetDateTime.now());
        return scrapingJobRepository.save(job);
    }

    /** Close a job the current tenant owns, recording what the import did. */
    @Transactional
    public ScrapingJob finishUploadJob(UUID jobId, String status, String resultSummaryJson,
                                       int companiesFound, String errorMessage) {
        ScrapingJob job = scrapingJobRepository.findByIdAndTenantId(jobId, TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Scraping job not found: " + jobId));
        job.setStatus(status);
        job.setResultSummary(resultSummaryJson);
        job.setCompaniesFound(companiesFound);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(OffsetDateTime.now());
        return scrapingJobRepository.save(job);
    }
}
