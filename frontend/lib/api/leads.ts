import { apiFetch } from "@/lib/api/client";
import type { BulkUpdateLeadStatusResponse, LeadStatus, TenantLead } from "@/lib/types/lead";

export function getLeads(): Promise<TenantLead[]> {
  return apiFetch<TenantLead[]>("/api/leads");
}

export function getLead(id: string): Promise<TenantLead> {
  return apiFetch<TenantLead>(`/api/leads/${id}`);
}

export function bulkUpdateLeadStatus(
  leadIds: string[],
  status: Extract<LeadStatus, "APPROVED" | "REJECTED">,
): Promise<BulkUpdateLeadStatusResponse> {
  return apiFetch<BulkUpdateLeadStatusResponse>("/api/leads/status/bulk", {
    method: "PATCH",
    body: JSON.stringify({ leadIds, status }),
  });
}

/**
 * Assigns leads to a campaign, or removes them from one when
 * `tenantCampaignId` is null. Only pending/approved leads can move — once a
 * lead has been emailed, its campaign no longer affects anything.
 */
export function bulkAssignLeadCampaign(
  leadIds: string[],
  tenantCampaignId: string | null,
): Promise<BulkUpdateLeadStatusResponse> {
  return apiFetch<BulkUpdateLeadStatusResponse>("/api/leads/campaign/bulk", {
    method: "PATCH",
    body: JSON.stringify({ leadIds, tenantCampaignId }),
  });
}
