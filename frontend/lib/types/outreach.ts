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
  toEmail: string;
  subject: string;
  status: OutreachEmailStatus;
  sentAt: string | null;
}
