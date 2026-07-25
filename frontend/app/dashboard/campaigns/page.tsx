import { Megaphone } from "lucide-react";

import { CampaignCard } from "@/components/campaigns/campaign-card";
import { EmptyState } from "@/components/shared/empty-state";
import { getCampaigns } from "@/lib/api/campaigns";

export default async function CampaignsPage() {
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
        title="Could not load campaigns"
        description="The backend API is unreachable. Make sure it is running at NEXT_PUBLIC_API_BASE_URL."
      />
    );
  }

  if (campaigns.length === 0) {
    return (
      <EmptyState
        icon={Megaphone}
        title="No campaigns yet"
        description="Create an outreach campaign to start emailing qualified leads."
      />
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {campaigns.map((campaign) => (
        <CampaignCard key={campaign.id} campaign={campaign} />
      ))}
    </div>
  );
}
