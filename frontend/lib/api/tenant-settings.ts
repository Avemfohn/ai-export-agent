import { apiFetch } from "@/lib/api/client";
import type {
  BuyerCriteria,
  CriteriaPreviewResponse,
  TenantSettingsResponse,
  UpdateTenantSettingsRequest,
} from "@/lib/types/tenant-settings";

export function getTenantSettings(): Promise<TenantSettingsResponse> {
  return apiFetch<TenantSettingsResponse>("/api/tenant-settings");
}

/** Partial update — any omitted field is left unchanged by the backend. */
export function updateTenantSettings(
  request: UpdateTenantSettingsRequest,
): Promise<TenantSettingsResponse> {
  return apiFetch<TenantSettingsResponse>("/api/tenant-settings", {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export function updateAutoApproveThreshold(threshold: number | null): Promise<TenantSettingsResponse> {
  return apiFetch<TenantSettingsResponse>("/api/tenant-settings/auto-approve-threshold", {
    method: "PATCH",
    body: JSON.stringify({ autoApproveThreshold: threshold }),
  });
}

/**
 * Scores a small sample of companies against candidate criteria without saving
 * them and without creating any leads. Omitted fields fall back to the
 * tenant's saved values.
 */
export function previewCriteria(request: {
  buyerCriteria?: BuyerCriteria;
  targetSectors?: string[];
  targetRegions?: string[];
}): Promise<CriteriaPreviewResponse> {
  return apiFetch<CriteriaPreviewResponse>("/api/leads/score/preview", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
