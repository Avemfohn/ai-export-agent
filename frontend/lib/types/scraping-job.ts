// Mirrors the scraping_jobs table CHECK constraints (see V1__initial_schema.sql)
// and the raw ScrapingJob entity returned by ScrapingJobController.
export type ScrapingJobStatus =
  | "PENDING"
  | "RUNNING"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";

export type ScrapingJobSource =
  | "GOOGLE_MAPS"
  | "B2B_DIRECTORY"
  | "TRADE_FAIR_UPLOAD"
  | "MANUAL";

export interface ScrapingJob {
  id: string;
  source: ScrapingJobSource;
  status: ScrapingJobStatus;
  companiesFound: number;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
}
