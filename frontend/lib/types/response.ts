export type ClassifiedIntent =
  | "INTERESTED"
  | "NOT_INTERESTED"
  | "NEEDS_INFO"
  | "OUT_OF_OFFICE"
  | "UNSUBSCRIBE"
  | "UNKNOWN";

export interface EmailResponse {
  id: string;
  fromEmail: string;
  subject: string;
  snippet: string;
  classifiedIntent: ClassifiedIntent;
  receivedAt: string;
}
