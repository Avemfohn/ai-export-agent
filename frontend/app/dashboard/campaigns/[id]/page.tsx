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
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

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

    return (
      <div className="space-y-6">
        <Card>
          <CardHeader className="flex-row items-start justify-between space-y-0">
            <CardTitle className="text-lg font-semibold text-foreground">
              {campaign.name}
            </CardTitle>
            <Badge variant="secondary">
              {dict.campaigns.status[campaign.status]}
            </Badge>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-foreground">
              {campaign.description ?? dict.campaigns.noDescription}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{dict.campaigns.detail.outreachEmails}</CardTitle>
          </CardHeader>
          <CardContent>
            <EmptyState title={dict.campaigns.detail.noEmails} />
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
