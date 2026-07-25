import { apiFetch } from "@/lib/api/client";
import type { EmailResponse } from "@/lib/types/response";

export function getEmailResponses(): Promise<EmailResponse[]> {
  return apiFetch<EmailResponse[]>("/api/email-responses");
}
