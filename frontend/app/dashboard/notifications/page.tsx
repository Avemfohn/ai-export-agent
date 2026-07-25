import { Bell } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { getNotifications } from "@/lib/api/notifications";
import type { NotificationChannel } from "@/lib/types/notification";
import { cn } from "@/lib/utils";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

const CHANNEL_VARIANT: Record<
  NotificationChannel,
  "success" | "slate" | "outline"
> = {
  WHATSAPP: "success",
  DASHBOARD: "slate",
  EMAIL: "outline",
};

export default async function NotificationsPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  let notifications: Awaited<ReturnType<typeof getNotifications>> = [];
  let errored = false;

  try {
    notifications = await getNotifications();
  } catch {
    errored = true;
  }

  if (errored) {
    return (
      <EmptyState
        icon={Bell}
        title={dict.notifications.errorTitle}
        description={dict.notifications.errorDescription}
      />
    );
  }

  if (notifications.length === 0) {
    return (
      <EmptyState
        icon={Bell}
        title={dict.notifications.emptyTitle}
        description={dict.notifications.emptyDescription}
      />
    );
  }

  return (
    <div className="space-y-3">
      {notifications.map((notification) => (
        <Card
          key={notification.id}
          className={cn(!notification.read && "border-primary/40")}
        >
          <CardContent className="flex items-start justify-between gap-4 p-4">
            <div className="min-w-0 space-y-1">
              <div className="flex items-center gap-2">
                <p className="font-medium text-foreground">
                  {notification.title}
                </p>
                <Badge variant={CHANNEL_VARIANT[notification.channel]}>
                  {dict.notifications.channel[notification.channel]}
                </Badge>
                {!notification.read ? (
                  <span className="h-2 w-2 rounded-full bg-primary" />
                ) : null}
              </div>
              <p className="text-sm text-muted-foreground">
                {notification.message ?? "—"}
              </p>
            </div>
            <p className="shrink-0 text-xs text-muted-foreground">
              {notification.sentAt
                ? new Date(notification.sentAt).toLocaleString()
                : "—"}
            </p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
