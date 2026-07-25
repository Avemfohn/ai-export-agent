export type CampaignStatus = "DRAFT" | "ACTIVE" | "PAUSED" | "COMPLETED";

export interface TenantCampaign {
  id: string;
  name: string;
  description: string | null;
  status: CampaignStatus;
  createdAt: string;
  updatedAt: string;
}
