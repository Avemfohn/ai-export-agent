import { notFound } from "next/navigation";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { LeadStatusBadge } from "@/components/leads/lead-status-badge";
import { EmptyState } from "@/components/shared/empty-state";
import { ApiError } from "@/lib/api/client";
import { getLead } from "@/lib/api/leads";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

export default async function LeadDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const locale = await getLocale();
  const dict = getDictionary(locale);

  try {
    const lead = await getLead(id);

    return (
      <div className="space-y-6">
        <Card>
          <CardHeader className="flex-row items-start justify-between space-y-0">
            <div>
              <CardTitle className="text-lg font-semibold text-foreground">
                {lead.companyName}
              </CardTitle>
              <p className="text-sm text-muted-foreground">
                {lead.domain}
              </p>
            </div>
            <LeadStatusBadge status={lead.status} dict={dict} />
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
            <div>
              <p className="text-muted-foreground">{dict.leads.detail.country}</p>
              <p className="font-medium text-foreground">
                {lead.country}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">{dict.leads.detail.sector}</p>
              <p className="font-medium text-foreground">
                {lead.sector}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">
                {dict.leads.detail.qualificationScore}
              </p>
              <p className="font-medium text-foreground">
                {lead.qualificationScore ?? "—"}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">{dict.leads.detail.lastUpdated}</p>
              <p className="font-medium text-foreground">
                {new Date(lead.updatedAt).toLocaleDateString()}
              </p>
            </div>
          </CardContent>
        </Card>

        {lead.qualificationNotes ? (
          <Card>
            <CardHeader>
              <CardTitle>{dict.leads.detail.qualificationNotes}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-foreground">
                {lead.qualificationNotes}
              </p>
            </CardContent>
          </Card>
        ) : null}

        <Card>
          <CardHeader>
            <CardTitle>{dict.leads.detail.outreachEmails}</CardTitle>
          </CardHeader>
          <CardContent>
            <EmptyState title={dict.leads.detail.noEmails} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{dict.leads.detail.responses}</CardTitle>
          </CardHeader>
          <CardContent>
            <EmptyState title={dict.leads.detail.noResponses} />
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
        title={dict.leads.detail.errorTitle}
        description={dict.leads.detail.errorDescription}
      />
    );
  }
}
