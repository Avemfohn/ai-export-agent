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
