export type NotificationChannel = "IN_APP" | "WHATSAPP" | "EMAIL";

export type NotificationType =
  | "WARM_REPLY"
  | "SCRAPING_JOB_COMPLETE"
  | "SCRAPING_JOB_FAILED"
  | "CAMPAIGN_UPDATE"
  | "SYSTEM";

export interface Notification {
  id: string;
  type: NotificationType;
  channel: NotificationChannel;
  title: string;
  message: string;
  isRead: boolean;
  sentAt: string;
}
