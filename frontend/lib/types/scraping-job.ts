export type ScrapingJobStatus =
  | "QUEUED"
  | "RUNNING"
  | "COMPLETED"
  | "FAILED";

export type ScrapingJobSource =
  | "GOOGLE_MAPS"
  | "B2B_DIRECTORY"
  | "TRADE_FAIR_UPLOAD";

export interface ScrapingJob {
  id: string;
  source: ScrapingJobSource;
  status: ScrapingJobStatus;
  companiesFound: number;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
}
