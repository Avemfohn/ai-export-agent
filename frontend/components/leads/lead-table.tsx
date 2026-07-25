import Link from "next/link";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { LeadStatusBadge } from "@/components/leads/lead-status-badge";
import type { TenantLead } from "@/lib/types/lead";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export function LeadTable({
  leads,
  dict,
}: {
  leads: TenantLead[];
  dict: Dictionary;
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{dict.leads.table.company}</TableHead>
          <TableHead>{dict.leads.table.domain}</TableHead>
          <TableHead>{dict.leads.table.country}</TableHead>
          <TableHead>{dict.leads.table.sector}</TableHead>
          <TableHead>{dict.leads.table.status}</TableHead>
          <TableHead className="text-right">{dict.leads.table.score}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {leads.map((lead) => (
          <TableRow key={lead.id}>
            <TableCell className="font-medium text-foreground">
              <Link
                href={`/dashboard/leads/${lead.id}`}
                className="hover:underline"
              >
                {lead.companyName}
              </Link>
            </TableCell>
            <TableCell className="text-muted-foreground">
              {lead.domain}
            </TableCell>
            <TableCell className="text-muted-foreground">
              {lead.country}
            </TableCell>
            <TableCell className="text-muted-foreground">
              {lead.sector}
            </TableCell>
            <TableCell>
              <LeadStatusBadge status={lead.status} dict={dict} />
            </TableCell>
            <TableCell className="text-right text-muted-foreground">
              {lead.qualificationScore ?? "—"}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
