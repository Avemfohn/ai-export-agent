// Mirrors backend/.../tenant/outreach/dto/EmailResponseDto.java and the
// email_responses table CHECK constraint (see V1__initial_schema.sql).
export type ClassifiedIntent =
  | "INTERESTED"
  | "NOT_INTERESTED"
  | "NEEDS_INFO"
  | "OUT_OF_OFFICE"
  | "UNSUBSCRIBE"
  | "SPAM"
  | "UNKNOWN";

export interface EmailResponse {
  id: string;
  outreachEmailId: string;
  fromEmail: string | null;
  subject: string | null;
  body: string | null;
  classifiedIntent: ClassifiedIntent | null;
  receivedAt: string;
}
