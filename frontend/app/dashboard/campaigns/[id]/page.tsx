import Link from "next/link";
import { notFound } from "next/navigation";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { LeadStatusBadge } from "@/components/leads/lead-status-badge";
import { CampaignStatusControl } from "@/components/campaigns/campaign-status-control";
import { EmptyState } from "@/components/shared/empty-state";
import { ApiError } from "@/lib/api/client";
import { getCampaign } from "@/lib/api/campaigns";
import { getLeads } from "@/lib/api/leads";
import type { TenantLead } from "@/lib/types/lead";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

/**
 * No per-campaign lead endpoint yet, so filter the tenant's list — the same
 * approach the lead detail page uses for its outreach emails, and fine at
 * mock-data scale.
 */
async function getLeadsForCampaign(campaignId: string): Promise<TenantLead[]> {
  try {
    const leads = await getLeads();
    return leads.filter((lead) => lead.tenantCampaignId === campaignId);
  } catch {
    return [];
  }
}

export default async function CampaignDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const locale = await getLocale();
  const dict = getDictionary(locale);

  try {
    const campaign = await getCampaign(id);
    const leads = await getLeadsForCampaign(id);
    // Only these are actually waiting on the campaign to send.
    const waitingCount = leads.filter((lead) => lead.status === "APPROVED").length;

    return (
      <div className="space-y-6">
        <Card>
          <CardHeader className="flex-row items-start justify-between space-y-0">
            <div>
              <CardTitle className="text-lg font-semibold text-foreground">
                {campaign.name}
              </CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">
                {campaign.description || dict.campaigns.noDescription}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <Badge variant={campaign.status === "ACTIVE" ? "success" : "slate"}>
                {dict.campaigns.status[campaign.status]}
              </Badge>
              <Button asChild variant="outline" size="sm">
                <Link href={`/dashboard/campaigns/${campaign.id}/edit`}>
                  {dict.campaigns.edit}
                </Link>
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <CampaignStatusControl
              campaignId={campaign.id}
              currentStatus={campaign.status}
              affectedLeadCount={waitingCount}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{dict.campaigns.detail.leads}</CardTitle>
            <CardDescription>{dict.campaigns.detail.leadsHelp}</CardDescription>
          </CardHeader>
          <CardContent>
            {leads.length === 0 ? (
              <EmptyState title={dict.campaigns.detail.noLeads} />
            ) : (
              <ul className="space-y-2">
                {leads.map((lead) => (
                  <li
                    key={lead.id}
                    className="flex items-center justify-between gap-3 rounded-md border border-input bg-muted/40 px-3 py-2"
                  >
                    <Link
                      href={`/dashboard/leads/${lead.id}`}
                      className="min-w-0 hover:underline"
                    >
                      <p className="text-sm font-medium text-foreground">{lead.companyName}</p>
                      <p className="text-xs text-muted-foreground">{lead.domain}</p>
                    </Link>
                    <LeadStatusBadge status={lead.status} dict={dict} />
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    );
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    return (
      <EmptyState
        title={dict.campaigns.detail.errorTitle}
        description={dict.campaigns.detail.errorDescription}
      />
    );
  }
}
