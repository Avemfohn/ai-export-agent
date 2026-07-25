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

export function LeadTable({ leads }: { leads: TenantLead[] }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Company</TableHead>
          <TableHead>Domain</TableHead>
          <TableHead>Country</TableHead>
          <TableHead>Sector</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="text-right">Score</TableHead>
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
                {lead.supplier.companyName}
              </Link>
            </TableCell>
            <TableCell className="text-muted-foreground">
              {lead.supplier.domain}
            </TableCell>
            <TableCell className="text-muted-foreground">
              {lead.supplier.country}
            </TableCell>
            <TableCell className="text-muted-foreground">
              {lead.supplier.sector}
            </TableCell>
            <TableCell>
              <LeadStatusBadge status={lead.status} />
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
