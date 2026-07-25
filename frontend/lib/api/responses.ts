import { apiFetch } from "@/lib/api/client";
import type { EmailResponse } from "@/lib/types/response";

// NOTE: the backend does not yet document a dedicated email-responses
// endpoint as of Sprint 1 (see CLAUDE.md — ai/scraping/email packages are
// placeholders). This points at a reasonable REST path following the same
// convention as the other resources; adjust once the backend contract lands.
export function getEmailResponses(): Promise<EmailResponse[]> {
  return apiFetch<EmailResponse[]>("/api/email-responses");
}
