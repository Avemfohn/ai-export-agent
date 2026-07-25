import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { TenantCampaign } from "@/lib/types/campaign";

const STATUS_VARIANT: Record<
  TenantCampaign["status"],
  "success" | "warning" | "slate" | "destructive"
> = {
  ACTIVE: "success",
  DRAFT: "slate",
  PAUSED: "warning",
  COMPLETED: "slate",
  ARCHIVED: "slate",
};

export function CampaignCard({ campaign }: { campaign: TenantCampaign }) {
  return (
    <Link href={`/dashboard/campaigns/${campaign.id}`}>
      <Card className="h-full transition-shadow hover:shadow-md">
        <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
          <CardTitle className="text-base font-semibold text-foreground">
            {campaign.name}
          </CardTitle>
          <Badge variant={STATUS_VARIANT[campaign.status]}>
            {campaign.status}
          </Badge>
        </CardHeader>
        <CardContent>
          <CardDescription>
            {campaign.description ?? "No description provided."}
          </CardDescription>
        </CardContent>
      </Card>
    </Link>
  );
}
