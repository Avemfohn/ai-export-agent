import { Badge, type BadgeProps } from "@/components/ui/badge";
import type { LeadStatus } from "@/lib/types/lead";
import type { Dictionary } from "@/lib/i18n/dictionaries";

const STATUS_VARIANT: Record<LeadStatus, NonNullable<BadgeProps["variant"]>> = {
  PENDING_APPROVAL: "warning",
  APPROVED: "slate",
  REJECTED: "destructive",
  EMAIL_SENT: "slate",
  NO_RESPONSE: "slate",
  INTERESTED: "success",
  NOT_INTERESTED: "destructive",
  BOUNCED: "destructive",
  CONVERTED: "success",
};

export function LeadStatusBadge({
  status,
  dict,
}: {
  status: LeadStatus;
  dict: Dictionary;
}) {
  return (
    <Badge variant={STATUS_VARIANT[status]}>{dict.leads.status[status]}</Badge>
  );
}
