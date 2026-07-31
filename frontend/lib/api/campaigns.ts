import { apiFetch } from "@/lib/api/client";
import type {
  CampaignStatus,
  CreateCampaignRequest,
  TenantCampaign,
  UpdateCampaignRequest,
} from "@/lib/types/campaign";

export function getCampaigns(): Promise<TenantCampaign[]> {
  return apiFetch<TenantCampaign[]>("/api/campaigns");
}

export function getCampaign(id: string): Promise<TenantCampaign> {
  return apiFetch<TenantCampaign>(`/api/campaigns/${id}`);
}

/** The campaign's email template starts as a copy of the tenant default. */
export function createCampaign(request: CreateCampaignRequest): Promise<TenantCampaign> {
  return apiFetch<TenantCampaign>("/api/campaigns", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

/** PUT: full replacement. Status is changed separately. */
export function updateCampaign(
  id: string,
  request: UpdateCampaignRequest,
): Promise<TenantCampaign> {
  return apiFetch<TenantCampaign>(`/api/campaigns/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

/** 409 if the transition isn't legal. */
export function updateCampaignStatus(
  id: string,
  status: CampaignStatus,
): Promise<TenantCampaign> {
  return apiFetch<TenantCampaign>(`/api/campaigns/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}
