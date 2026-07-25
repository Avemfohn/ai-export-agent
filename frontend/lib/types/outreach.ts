export type OutreachEmailStatus =
  | "QUEUED"
  | "SENT"
  | "DELIVERED"
  | "OPENED"
  | "FAILED"
  | "BOUNCED";

export interface OutreachEmail {
  id: string;
  toEmail: string;
  subject: string;
  status: OutreachEmailStatus;
  sentAt: string | null;
}
