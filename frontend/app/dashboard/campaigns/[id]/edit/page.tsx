import { notFound } from "next/navigation";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CampaignForm } from "@/components/campaigns/campaign-form";
import { EmptyState } from "@/components/shared/empty-state";
import { ApiError } from "@/lib/api/client";
import { getCampaign } from "@/lib/api/campaigns";
import { getTenantSettings } from "@/lib/api/tenant-settings";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

export default async function EditCampaignPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const locale = await getLocale();
  const dict = getDictionary(locale);

  try {
    const campaign = await getCampaign(id);

    let senderName: string | null = null;
    try {
      senderName = (await getTenantSettings()).emailSenderName ?? null;
    } catch {
      // Non-critical — the preview just shows an unsigned email.
    }

    return (
      <div className="max-w-3xl">
        <Card>
          <CardHeader>
            <CardTitle>{dict.campaigns.form.editTitle}</CardTitle>
            <CardDescription>{dict.campaigns.form.editDescription}</CardDescription>
          </CardHeader>
          <CardContent>
            <CampaignForm campaign={campaign} senderName={senderName} />
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
