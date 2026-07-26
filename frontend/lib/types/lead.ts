export type LeadStatus =
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "REJECTED"
  | "EMAIL_SENT"
  | "NO_RESPONSE"
  | "INTERESTED"
  | "NOT_INTERESTED"
  | "BOUNCED"
  | "CONVERTED";

// Mirrors backend/.../tenant/lead/dto/TenantLeadResponse.java exactly — the
// joined GlobalSupplier fields are flattened onto the lead response, not
// nested under a `supplier` object.
export interface TenantLead {
  id: string;
  tenantCampaignId: string | null;
  status: LeadStatus;
  qualificationScore: number | null;
  qualificationNotes: string | null;
  createdAt: string;
  updatedAt: string;
  globalSupplierId: string;
  companyName: string;
  domain: string;
  country: string;
  sector: string;
}

// Mirrors backend/.../tenant/lead/dto/BulkUpdateLeadStatusResponse.java
export interface BulkUpdateLeadStatusResponse {
  requested: number;
  updated: number;
  skippedWrongStatus: number;
  notFoundOrForeignTenant: number;
}
