// Mirrors backend/.../tenant/account/dto/TenantSettingsResponse.java.
//
// The backend sets Jackson's default-property-inclusion to non_null, so ANY
// field can be absent from the payload rather than null — every field here is
// optional for that reason, and consumers must use truthiness / `?? null`
// rather than `=== null`.

/** buyer_criteria is deliberately schema-free so the AI reads it holistically. */
export type BuyerCriteria = Record<string, unknown>;

export interface EmailDraftTemplate {
  subject?: string;
  body?: string;
  notes?: string;
  // Unknown keys are preserved by the backend and must survive a round-trip.
  [key: string]: unknown;
}

export interface TenantSettingsResponse {
  buyerCriteria?: BuyerCriteria | null;
  targetSectors?: string[] | null;
  targetRegions?: string[] | null;
  emailDraftTemplate?: EmailDraftTemplate | null;
  /** Read-only: configured at onboarding, coupled to SPF/DKIM and domain reputation. */
  emailSenderName?: string | null;
  /** Read-only, same reason. */
  emailSenderAddress?: string | null;
  autoApproveThreshold?: number | null;
}

/** Any omitted field is left unchanged by the backend. */
export interface UpdateTenantSettingsRequest {
  buyerCriteria?: BuyerCriteria;
  targetSectors?: string[];
  targetRegions?: string[];
  emailDraftTemplate?: EmailDraftTemplate;
}

export interface ScoredSample {
  companyName: string;
  domain: string;
  country?: string | null;
  sector?: string | null;
  score: number;
  rationale?: string | null;
  wouldApprove: boolean;
  wouldReject: boolean;
}

export interface CriteriaPreviewResponse {
  sampleSize: number;
  samples: ScoredSample[];
}
