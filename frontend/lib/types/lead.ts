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

export interface GlobalSupplier {
  id: string;
  companyName: string;
  domain: string;
  country: string;
  sector: string;
}

export interface TenantLead {
  id: string;
  status: LeadStatus;
  qualificationScore: number | null;
  qualificationNotes: string | null;
  supplier: GlobalSupplier;
  createdAt: string;
  updatedAt: string;
}
