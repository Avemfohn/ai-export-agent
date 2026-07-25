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
import { getOutreachEmails } from "@/lib/api/outreach";
import type { OutreachEmailStatus } from "@/lib/types/outreach";

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
        title="Could not load outreach emails"
        description="The backend API is unreachable. Make sure it is running at NEXT_PUBLIC_API_BASE_URL."
      />
    );
  }

  if (emails.length === 0) {
    return (
      <EmptyState
        icon={Send}
        title="No outreach emails sent yet"
        description="Emails sent to qualified leads will show up here."
      />
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>To</TableHead>
          <TableHead>Subject</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="text-right">Sent At</TableHead>
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
            </TableCell>
            <TableCell>
              <Badge variant={STATUS_VARIANT[email.status]}>
                {email.status}
              </Badge>
            </TableCell>
            <TableCell className="text-right text-muted-foreground">
              {email.sentAt ? new Date(email.sentAt).toLocaleString() : "—"}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
