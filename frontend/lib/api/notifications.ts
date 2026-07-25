import { apiFetch } from "@/lib/api/client";
import type { Notification } from "@/lib/types/notification";

export function getNotifications(): Promise<Notification[]> {
  return apiFetch<Notification[]>("/api/notifications");
}
