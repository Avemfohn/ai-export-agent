// Mirrors backend/.../tenant/outreach/dto/OutreachEmailResponse.java and the
// outreach_emails table CHECK constraint (see V1__initial_schema.sql).
export type OutreachEmailStatus =
  | "DRAFT"
  | "QUEUED"
  | "SENT"
  | "FAILED"
  | "BOUNCED";

export interface OutreachEmail {
  id: string;
  tenantLeadId: string;
  toEmail: string;
  subject: string;
  status: OutreachEmailStatus;
  // Optional, not just nullable: the backend sets Jackson's
  // default-property-inclusion to non_null, so these keys are absent from the
  // JSON rather than null. Always test with truthiness, never `=== null`.
  errorMessage?: string | null;
  sentAt?: string | null;
}
