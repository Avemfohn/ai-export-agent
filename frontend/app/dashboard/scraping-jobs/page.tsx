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

const STATUS_VARIANT: Record<
  ScrapingJobStatus,
  "success" | "warning" | "slate" | "destructive"
> = {
  QUEUED: "slate",
  RUNNING: "warning",
  COMPLETED: "success",
  FAILED: "destructive",
};

export default async function ScrapingJobsPage() {
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
        title="Could not load scraping jobs"
        description="The backend API is unreachable. Make sure it is running at NEXT_PUBLIC_API_BASE_URL."
      />
    );
  }

  if (jobs.length === 0) {
    return (
      <EmptyState
        icon={Radar}
        title="No scraping jobs yet"
        description="Jobs that gather target-sector companies from Google Maps, B2B directories, and trade-fair uploads will appear here."
      />
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Source</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="text-right">Companies Found</TableHead>
          <TableHead>Error</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {jobs.map((job) => (
          <TableRow key={job.id}>
            <TableCell className="font-medium text-foreground">
              {job.source.replace(/_/g, " ")}
            </TableCell>
            <TableCell>
              <Badge variant={STATUS_VARIANT[job.status]}>{job.status}</Badge>
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
