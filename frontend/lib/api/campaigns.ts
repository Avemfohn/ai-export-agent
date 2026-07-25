import { apiFetch } from "@/lib/api/client";
import type { TenantCampaign } from "@/lib/types/campaign";

export function getCampaigns(): Promise<TenantCampaign[]> {
  return apiFetch<TenantCampaign[]>("/api/campaigns");
}

export function getCampaign(id: string): Promise<TenantCampaign> {
  return apiFetch<TenantCampaign>(`/api/campaigns/${id}`);
}
