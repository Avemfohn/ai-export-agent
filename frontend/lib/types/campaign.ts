import type { EmailDraftTemplate } from "@/lib/types/tenant-settings";

export type CampaignStatus =
  | "DRAFT"
  | "ACTIVE"
  | "PAUSED"
  | "COMPLETED"
  | "ARCHIVED";

/** Only ACTIVE campaigns have their leads drafted and queued for outreach. */
export const SENDING_STATUS: CampaignStatus = "ACTIVE";

/** Statuses a campaign can be created as — see CreateTenantCampaignRequest. */
export const CREATABLE_STATUSES: CampaignStatus[] = ["DRAFT", "ACTIVE"];

/**
 * Mirrors CampaignStatus.canTransitionTo on the backend. The backend stays the
 * authority (409 on an illegal transition); this only keeps the UI from
 * offering options that would be rejected.
 */
export const ALLOWED_TRANSITIONS: Record<CampaignStatus, CampaignStatus[]> = {
  DRAFT: ["ACTIVE", "ARCHIVED"],
  ACTIVE: ["PAUSED", "COMPLETED", "ARCHIVED"],
  PAUSED: ["ACTIVE", "COMPLETED", "ARCHIVED"],
  COMPLETED: ["ARCHIVED"],
  ARCHIVED: ["DRAFT"],
};

export interface TenantCampaign {
  id: string;
  name: string;
  // Nullable in the database, and the backend omits nulls entirely
  // (default-property-inclusion: non_null) — use truthiness, never `=== null`.
  description?: string | null;
  status: CampaignStatus;
  buyerCriteriaSnapshot?: Record<string, unknown> | null;
  emailDraftTemplateSnapshot?: EmailDraftTemplate | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCampaignRequest {
  name: string;
  description?: string | null;
  status?: CampaignStatus;
}

/** PUT — full replacement, so every field is sent every time. */
export interface UpdateCampaignRequest {
  name: string;
  description?: string | null;
  emailDraftTemplate?: EmailDraftTemplate;
}
