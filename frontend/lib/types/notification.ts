// Mirrors the notifications table CHECK constraints (see V1__initial_schema.sql)
// and the raw Notification entity returned by NotificationController.
export type NotificationChannel = "DASHBOARD" | "WHATSAPP" | "EMAIL";

export type NotificationType =
  | "WARM_REPLY"
  | "NEW_LEAD"
  | "SCRAPING_JOB_DONE"
  | "BOUNCE_ALERT"
  | "SYSTEM";

export interface Notification {
  id: string;
  type: NotificationType;
  channel: NotificationChannel;
  title: string;
  message: string | null;
  read: boolean;
  sentAt: string | null;
}
