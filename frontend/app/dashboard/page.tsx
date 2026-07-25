import { Users, MessageSquareText, Megaphone, Radar } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getLeads } from "@/lib/api/leads";
import { getCampaigns } from "@/lib/api/campaigns";
import { getEmailResponses } from "@/lib/api/responses";
import { getScrapingJobs } from "@/lib/api/scraping-jobs";

interface StatResult {
  value: number | null;
  errored: boolean;
}

async function safeCount(fn: () => Promise<unknown[]>): Promise<StatResult> {
  try {
    const data = await fn();
    return { value: data.length, errored: false };
  } catch {
    return { value: null, errored: true };
  }
}

async function safeCountWhere<T>(
  fn: () => Promise<T[]>,
  predicate: (item: T) => boolean,
): Promise<StatResult> {
  try {
    const data = await fn();
    return { value: data.filter(predicate).length, errored: false };
  } catch {
    return { value: null, errored: true };
  }
}

function StatCard({
  label,
  icon: Icon,
  result,
}: {
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  result: StatResult;
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle>{label}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-semibold text-foreground">
          {result.value ?? "—"}
        </div>
        {result.errored ? (
          <p className="mt-1 text-xs text-muted-foreground">
            Backend unavailable
          </p>
        ) : null}
      </CardContent>
    </Card>
  );
}

export default async function DashboardOverviewPage() {
  const [totalLeads, warmReplies, activeCampaigns, scrapingJobs] =
    await Promise.all([
      safeCount(getLeads),
      safeCountWhere(getEmailResponses, (r) => r.classifiedIntent === "INTERESTED"),
      safeCountWhere(getCampaigns, (c) => c.status === "ACTIVE"),
      safeCount(getScrapingJobs),
    ]);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total Leads" icon={Users} result={totalLeads} />
        <StatCard
          label="Warm Replies"
          icon={MessageSquareText}
          result={warmReplies}
        />
        <StatCard
          label="Active Campaigns"
          icon={Megaphone}
          result={activeCampaigns}
        />
        <StatCard label="Scraping Jobs" icon={Radar} result={scrapingJobs} />
      </div>
      <p className="text-sm text-muted-foreground">
        Data is fetched live from the backend API on each load. If a card
        shows a dash, the backend was unreachable at request time.
      </p>
    </div>
  );
}
