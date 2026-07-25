import { Radar } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { EmptyState } from "@/components/shared/empty-state";
import { getScrapingJobs } from "@/lib/api/scraping-jobs";
import type { ScrapingJobStatus } from "@/lib/types/scraping-job";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

const STATUS_VARIANT: Record<
  ScrapingJobStatus,
  "success" | "warning" | "slate" | "destructive"
> = {
  PENDING: "slate",
  RUNNING: "warning",
  COMPLETED: "success",
  FAILED: "destructive",
  CANCELLED: "slate",
};

export default async function ScrapingJobsPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  let jobs: Awaited<ReturnType<typeof getScrapingJobs>> = [];
  let errored = false;

  try {
    jobs = await getScrapingJobs();
  } catch {
    errored = true;
  }

  if (errored) {
    return (
      <EmptyState
        icon={Radar}
        title={dict.scrapingJobs.errorTitle}
        description={dict.scrapingJobs.errorDescription}
      />
    );
  }

  if (jobs.length === 0) {
    return (
      <EmptyState
        icon={Radar}
        title={dict.scrapingJobs.emptyTitle}
        description={dict.scrapingJobs.emptyDescription}
      />
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{dict.scrapingJobs.table.source}</TableHead>
          <TableHead>{dict.scrapingJobs.table.status}</TableHead>
          <TableHead className="text-right">
            {dict.scrapingJobs.table.companiesFound}
          </TableHead>
          <TableHead>{dict.scrapingJobs.table.error}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {jobs.map((job) => (
          <TableRow key={job.id}>
            <TableCell className="font-medium text-foreground">
              {dict.scrapingJobs.source[job.source]}
            </TableCell>
            <TableCell>
              <Badge variant={STATUS_VARIANT[job.status]}>
                {dict.scrapingJobs.status[job.status]}
              </Badge>
            </TableCell>
            <TableCell className="text-right text-muted-foreground">
              {job.companiesFound}
            </TableCell>
            <TableCell className="text-sm text-destructive">
              {job.errorMessage ?? "—"}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
