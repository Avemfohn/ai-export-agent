import { apiFetch } from "@/lib/api/client";
import type { TenantSettingsResponse } from "@/lib/types/tenant-settings";

export function getTenantSettings(): Promise<TenantSettingsResponse> {
  return apiFetch<TenantSettingsResponse>("/api/tenant-settings");
}

export function updateAutoApproveThreshold(threshold: number | null): Promise<TenantSettingsResponse> {
  return apiFetch<TenantSettingsResponse>("/api/tenant-settings/auto-approve-threshold", {
    method: "PATCH",
    body: JSON.stringify({ autoApproveThreshold: threshold }),
  });
}
