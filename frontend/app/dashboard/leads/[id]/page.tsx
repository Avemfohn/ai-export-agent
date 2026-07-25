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

export default async function LeadDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  try {
    const lead = await getLead(id);

    return (
      <div className="space-y-6">
        <Card>
          <CardHeader className="flex-row items-start justify-between space-y-0">
            <div>
              <CardTitle className="text-lg font-semibold text-foreground">
                {lead.supplier.companyName}
              </CardTitle>
              <p className="text-sm text-muted-foreground">
                {lead.supplier.domain}
              </p>
            </div>
            <LeadStatusBadge status={lead.status} />
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
            <div>
              <p className="text-muted-foreground">Country</p>
              <p className="font-medium text-foreground">
                {lead.supplier.country}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">Sector</p>
              <p className="font-medium text-foreground">
                {lead.supplier.sector}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">Qualification Score</p>
              <p className="font-medium text-foreground">
                {lead.qualificationScore ?? "—"}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">Last Updated</p>
              <p className="font-medium text-foreground">
                {new Date(lead.updatedAt).toLocaleDateString()}
              </p>
            </div>
          </CardContent>
        </Card>

        {lead.qualificationNotes ? (
          <Card>
            <CardHeader>
              <CardTitle>Qualification Notes</CardTitle>
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
            <CardTitle>Outreach Emails</CardTitle>
          </CardHeader>
          <CardContent>
            <EmptyState title="No emails sent to this lead yet" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Responses</CardTitle>
          </CardHeader>
          <CardContent>
            <EmptyState title="No responses recorded yet" />
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
        title="Could not load this lead"
        description="The backend API is unreachable. Make sure it is running at NEXT_PUBLIC_API_BASE_URL."
      />
    );
  }
}
