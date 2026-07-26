// Mirrors backend/.../tenant/account/dto/TenantSettingsResponse.java —
// intentionally minimal, only what this feature needs.
export interface TenantSettingsResponse {
  autoApproveThreshold: number | null;
}
