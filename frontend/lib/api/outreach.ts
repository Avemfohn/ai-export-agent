import { apiFetch } from "@/lib/api/client";
import type { OutreachEmail } from "@/lib/types/outreach";

export function getOutreachEmails(): Promise<OutreachEmail[]> {
  return apiFetch<OutreachEmail[]>("/api/outreach-emails");
}
