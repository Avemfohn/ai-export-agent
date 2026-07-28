import { Send } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { EmptyState } from "@/components/shared/empty-state";
import { RequeueButton } from "@/components/outreach/requeue-button";
import { getOutreachEmails } from "@/lib/api/outreach";
import type { OutreachEmailStatus } from "@/lib/types/outreach";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

const STATUS_VARIANT: Record<
  OutreachEmailStatus,
  "success" | "warning" | "slate" | "destructive"
> = {
  DRAFT: "slate",
  QUEUED: "warning",
  SENT: "success",
  FAILED: "destructive",
  BOUNCED: "destructive",
};

export default async function OutreachPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  let emails: Awaited<ReturnType<typeof getOutreachEmails>> = [];
  let errored = false;

  try {
    emails = await getOutreachEmails();
  } catch {
    errored = true;
  }

  if (errored) {
    return (
      <EmptyState
        icon={Send}
        title={dict.outreach.errorTitle}
        description={dict.outreach.errorDescription}
      />
    );
  }

  if (emails.length === 0) {
    return (
      <EmptyState
        icon={Send}
        title={dict.outreach.emptyTitle}
        description={dict.outreach.emptyDescription}
      />
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{dict.outreach.table.to}</TableHead>
          <TableHead>{dict.outreach.table.subject}</TableHead>
          <TableHead>{dict.outreach.table.status}</TableHead>
          <TableHead className="text-right">{dict.outreach.table.sentAt}</TableHead>
          <TableHead className="text-right">{dict.outreach.table.actions}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {emails.map((email) => (
          <TableRow key={email.id}>
            <TableCell className="font-medium text-foreground">
              {email.toEmail}
            </TableCell>
            <TableCell className="text-muted-foreground">
              {email.subject}
              {email.errorMessage ? (
                <p className="mt-1 text-xs text-destructive">
                  {email.errorMessage}
                </p>
              ) : null}
            </TableCell>
            <TableCell>
              <Badge variant={STATUS_VARIANT[email.status]}>
                {dict.outreach.status[email.status]}
              </Badge>
            </TableCell>
            <TableCell className="text-right text-muted-foreground">
              {email.sentAt ? new Date(email.sentAt).toLocaleString() : "—"}
            </TableCell>
            <TableCell className="text-right">
              {email.status === "FAILED" ? (
                <RequeueButton emailId={email.id} />
              ) : null}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
