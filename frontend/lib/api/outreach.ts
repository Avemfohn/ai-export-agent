import { apiFetch } from "@/lib/api/client";
import type { OutreachEmail } from "@/lib/types/outreach";

export function getOutreachEmails(): Promise<OutreachEmail[]> {
  return apiFetch<OutreachEmail[]>("/api/outreach-emails");
}

/** Puts a FAILED email back on the send queue. 409 if it isn't FAILED. */
export function requeueOutreachEmail(id: string): Promise<OutreachEmail> {
  return apiFetch<OutreachEmail>(`/api/outreach-emails/${id}/requeue`, {
    method: "POST",
  });
}
