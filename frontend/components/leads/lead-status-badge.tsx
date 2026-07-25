import { Badge, type BadgeProps } from "@/components/ui/badge";
import type { LeadStatus } from "@/lib/types/lead";

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

const STATUS_LABEL: Record<LeadStatus, string> = {
  PENDING_APPROVAL: "Pending Approval",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  EMAIL_SENT: "Email Sent",
  NO_RESPONSE: "No Response",
  INTERESTED: "Interested",
  NOT_INTERESTED: "Not Interested",
  BOUNCED: "Bounced",
  CONVERTED: "Converted",
};

export function LeadStatusBadge({ status }: { status: LeadStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{STATUS_LABEL[status]}</Badge>;
}
