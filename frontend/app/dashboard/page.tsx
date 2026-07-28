import { Users, MessageSquareText, Megaphone, Radar, MailWarning } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getLeads } from "@/lib/api/leads";
import { getCampaigns } from "@/lib/api/campaigns";
import { getEmailResponses } from "@/lib/api/responses";
import { getOutreachEmails } from "@/lib/api/outreach";
import { getScrapingJobs } from "@/lib/api/scraping-jobs";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

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
  backendUnavailableLabel,
}: {
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  result: StatResult;
  backendUnavailableLabel: string;
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
            {backendUnavailableLabel}
          </p>
        ) : null}
      </CardContent>
    </Card>
  );
}

export default async function DashboardOverviewPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  const [totalLeads, warmReplies, activeCampaigns, scrapingJobs, failedSends] =
    await Promise.all([
      safeCount(getLeads),
      safeCountWhere(getEmailResponses, (r) => r.classifiedIntent === "INTERESTED"),
      safeCountWhere(getCampaigns, (c) => c.status === "ACTIVE"),
      safeCount(getScrapingJobs),
      safeCountWhere(getOutreachEmails, (e) => e.status === "FAILED"),
    ]);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label={dict.overview.totalLeads}
          icon={Users}
          result={totalLeads}
          backendUnavailableLabel={dict.overview.backendUnavailable}
        />
        <StatCard
          label={dict.overview.warmReplies}
          icon={MessageSquareText}
          result={warmReplies}
          backendUnavailableLabel={dict.overview.backendUnavailable}
        />
        <StatCard
          label={dict.overview.activeCampaigns}
          icon={Megaphone}
          result={activeCampaigns}
          backendUnavailableLabel={dict.overview.backendUnavailable}
        />
        <StatCard
          label={dict.overview.scrapingJobs}
          icon={Radar}
          result={scrapingJobs}
          backendUnavailableLabel={dict.overview.backendUnavailable}
        />
        <StatCard
          label={dict.overview.failedSends}
          icon={MailWarning}
          result={failedSends}
          backendUnavailableLabel={dict.overview.backendUnavailable}
        />
      </div>
      <p className="text-sm text-muted-foreground">{dict.overview.footnote}</p>
    </div>
  );
}
