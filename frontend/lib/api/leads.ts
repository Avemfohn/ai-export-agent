import { apiFetch } from "@/lib/api/client";
import type { TenantLead } from "@/lib/types/lead";

export function getLeads(): Promise<TenantLead[]> {
  return apiFetch<TenantLead[]>("/api/leads");
}

export function getLead(id: string): Promise<TenantLead> {
  return apiFetch<TenantLead>(`/api/leads/${id}`);
}
