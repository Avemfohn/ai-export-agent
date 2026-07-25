import { notFound } from "next/navigation";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { ApiError } from "@/lib/api/client";
import { getCampaign } from "@/lib/api/campaigns";

export default async function CampaignDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  try {
    const campaign = await getCampaign(id);

    return (
      <div className="space-y-6">
        <Card>
          <CardHeader className="flex-row items-start justify-between space-y-0">
            <CardTitle className="text-lg font-semibold text-foreground">
              {campaign.name}
            </CardTitle>
            <Badge variant="secondary">{campaign.status}</Badge>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-foreground">
              {campaign.description ?? "No description provided."}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Outreach Emails</CardTitle>
          </CardHeader>
          <CardContent>
            <EmptyState title="No outreach emails linked to this campaign yet" />
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
        title="Could not load this campaign"
        description="The backend API is unreachable. Make sure it is running at NEXT_PUBLIC_API_BASE_URL."
      />
    );
  }
}
