import { notFound } from "next/navigation";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { LeadStatusBadge } from "@/components/leads/lead-status-badge";
import { EmptyState } from "@/components/shared/empty-state";
import { RequeueButton } from "@/components/outreach/requeue-button";
import { ApiError } from "@/lib/api/client";
import { getLead } from "@/lib/api/leads";
import { getOutreachEmails } from "@/lib/api/outreach";
import type { OutreachEmail, OutreachEmailStatus } from "@/lib/types/outreach";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

const STATUS_VARIANT: Record<
  OutreachEmailStatus,
  "success" | "warning" | "slate" | "destructive"
> = {
  DRAFT: "slate",
  QUEUED: "warning",
  SENT: "success",
  FAILED: "destructive",
  BOUNCED: "destructive",
};

/**
 * There's no per-lead outreach endpoint yet, so filter the tenant's list
 * client-side — same approach the dashboard overview already takes, and fine
 * at mock-data scale. A failure here must not blank the whole lead page.
 */
async function getEmailsForLead(leadId: string): Promise<OutreachEmail[]> {
  try {
    const emails = await getOutreachEmails();
    return emails.filter((email) => email.tenantLeadId === leadId);
  } catch {
    return [];
  }
}

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
    const emails = await getEmailsForLead(id);

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
            {emails.length === 0 ? (
              <EmptyState title={dict.leads.detail.noEmails} />
            ) : (
              <ul className="space-y-4">
                {emails.map((email) => (
                  <li
                    key={email.id}
                    className="space-y-2 border-b border-border pb-4 last:border-0 last:pb-0"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-foreground">
                          {email.subject}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {email.toEmail}
                          {email.sentAt
                            ? ` · ${new Date(email.sentAt).toLocaleString()}`
                            : ""}
                        </p>
                      </div>
                      <Badge variant={STATUS_VARIANT[email.status]}>
                        {dict.outreach.status[email.status]}
                      </Badge>
                    </div>
                    {email.errorMessage ? (
                      <p className="text-xs text-destructive">
                        {dict.outreach.failureLabel}: {email.errorMessage}
                      </p>
                    ) : null}
                    {email.status === "FAILED" ? (
                      <RequeueButton emailId={email.id} />
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
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
