import { apiFetch } from "@/lib/api/client";
import type { ScrapingJob } from "@/lib/types/scraping-job";

export function getScrapingJobs(): Promise<ScrapingJob[]> {
  return apiFetch<ScrapingJob[]>("/api/scraping-jobs");
}
