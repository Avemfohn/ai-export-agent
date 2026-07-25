import { Megaphone } from "lucide-react";

import { CampaignCard } from "@/components/campaigns/campaign-card";
import { EmptyState } from "@/components/shared/empty-state";
import { getCampaigns } from "@/lib/api/campaigns";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

export default async function CampaignsPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  let campaigns: Awaited<ReturnType<typeof getCampaigns>> = [];
  let errored = false;

  try {
    campaigns = await getCampaigns();
  } catch {
    errored = true;
  }

  if (errored) {
    return (
      <EmptyState
        icon={Megaphone}
        title={dict.campaigns.errorTitle}
        description={dict.campaigns.errorDescription}
      />
    );
  }

  if (campaigns.length === 0) {
    return (
      <EmptyState
        icon={Megaphone}
        title={dict.campaigns.emptyTitle}
        description={dict.campaigns.emptyDescription}
      />
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {campaigns.map((campaign) => (
        <CampaignCard key={campaign.id} campaign={campaign} dict={dict} />
      ))}
    </div>
  );
}
