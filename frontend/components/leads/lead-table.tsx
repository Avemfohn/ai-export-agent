"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { ArrowDown, ArrowUp, ChevronsUpDown } from "lucide-react";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { LeadStatusBadge } from "@/components/leads/lead-status-badge";
import { LeadBulkToolbar } from "@/components/leads/lead-bulk-toolbar";
import type { LeadStatus, TenantLead } from "@/lib/types/lead";
import type { Dictionary } from "@/lib/i18n/dictionaries";

const ALL = "ALL";

type SortColumn = "companyName" | "domain" | "country" | "sector" | "status" | "qualificationScore";

export function LeadTable({
  leads,
  dict,
}: {
  leads: TenantLead[];
  dict: Dictionary;
}) {
  const [statusFilter, setStatusFilter] = useState<string>(ALL);
  const [sectorFilter, setSectorFilter] = useState<string>(ALL);
  const [scoreMin, setScoreMin] = useState<string>("");
  const [scoreMax, setScoreMax] = useState<string>("");
  const [sortColumn, setSortColumn] = useState<SortColumn | null>(null);
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  const statuses = Object.keys(dict.leads.status) as LeadStatus[];
  const sectors = useMemo(
    () => Array.from(new Set(leads.map((lead) => lead.sector))).sort(),
    [leads],
  );

  const filteredLeads = useMemo(() => {
    const min = scoreMin === "" ? null : Number(scoreMin);
    const max = scoreMax === "" ? null : Number(scoreMax);
    return leads.filter((lead) => {
      if (statusFilter !== ALL && lead.status !== statusFilter) return false;
      if (sectorFilter !== ALL && lead.sector !== sectorFilter) return false;
      if (min !== null && (lead.qualificationScore ?? -Infinity) < min) return false;
      if (max !== null && (lead.qualificationScore ?? Infinity) > max) return false;
      return true;
    });
  }, [leads, statusFilter, sectorFilter, scoreMin, scoreMax]);

  const sortedLeads = useMemo(() => {
    if (!sortColumn) return filteredLeads;
    const dir = sortDirection === "asc" ? 1 : -1;
    return [...filteredLeads].sort((a, b) => {
      const av = a[sortColumn] ?? "";
      const bv = b[sortColumn] ?? "";
      if (av < bv) return -1 * dir;
      if (av > bv) return 1 * dir;
      return 0;
    });
  }, [filteredLeads, sortColumn, sortDirection]);

  function toggleSort(column: SortColumn) {
    if (sortColumn !== column) {
      setSortColumn(column);
      setSortDirection("desc");
    } else {
      setSortDirection(sortDirection === "desc" ? "asc" : "desc");
    }
  }

  function sortIcon(column: SortColumn) {
    if (sortColumn !== column) return <ChevronsUpDown className="ml-1 inline h-3 w-3 opacity-50" />;
    return sortDirection === "asc" ? (
      <ArrowUp className="ml-1 inline h-3 w-3" />
    ) : (
      <ArrowDown className="ml-1 inline h-3 w-3" />
    );
  }

  // Only PENDING_APPROVAL leads can actually be approved/rejected (the
  // backend's review-gate rule) — restrict what "select all" and the header
  // checkbox can grab to that set, so already-processed leads can never be
  // selected for a bulk action in the first place.
  const selectablePendingLeads = sortedLeads.filter((lead) => lead.status === "PENDING_APPROVAL");
  const allFilteredSelected =
    selectablePendingLeads.length > 0 && selectablePendingLeads.every((lead) => selectedIds.has(lead.id));

  function toggleSelectAll() {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (allFilteredSelected) {
        selectablePendingLeads.forEach((lead) => next.delete(lead.id));
      } else {
        selectablePendingLeads.forEach((lead) => next.add(lead.id));
      }
      return next;
    });
  }

  function toggleRow(id: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function clearFilters() {
    setStatusFilter(ALL);
    setSectorFilter(ALL);
    setScoreMin("");
    setScoreMax("");
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {dict.leads.filters.statusLabel}
          </label>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-[180px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>{dict.leads.filters.allStatuses}</SelectItem>
              {statuses.map((status) => (
                <SelectItem key={status} value={status}>
                  {dict.leads.status[status]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {dict.leads.filters.sectorLabel}
          </label>
          <Select value={sectorFilter} onValueChange={setSectorFilter}>
            <SelectTrigger className="w-[180px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>{dict.leads.filters.allSectors}</SelectItem>
              {sectors.map((sector) => (
                <SelectItem key={sector} value={sector}>
                  {sector}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {dict.leads.filters.scoreMinLabel}
          </label>
          <Input
            type="number"
            className="w-[100px]"
            value={scoreMin}
            onChange={(e) => setScoreMin(e.target.value)}
          />
        </div>

        <div className="space-y-1">
          <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {dict.leads.filters.scoreMaxLabel}
          </label>
          <Input
            type="number"
            className="w-[100px]"
            value={scoreMax}
            onChange={(e) => setScoreMax(e.target.value)}
          />
        </div>

        <Button variant="ghost" size="sm" onClick={clearFilters}>
          {dict.leads.filters.clearFilters}
        </Button>
      </div>

      {selectedIds.size > 0 && (
        <LeadBulkToolbar
          selectedIds={Array.from(selectedIds)}
          dict={dict}
          onDone={() => setSelectedIds(new Set())}
        />
      )}

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-10">
              <Checkbox
                checked={allFilteredSelected}
                onCheckedChange={toggleSelectAll}
                disabled={selectablePendingLeads.length === 0}
                aria-label={dict.leads.bulk.selectAllHeader}
              />
            </TableHead>
            <TableHead className="cursor-pointer select-none" onClick={() => toggleSort("companyName")}>
              {dict.leads.table.company}
              {sortIcon("companyName")}
            </TableHead>
            <TableHead className="cursor-pointer select-none" onClick={() => toggleSort("domain")}>
              {dict.leads.table.domain}
              {sortIcon("domain")}
            </TableHead>
            <TableHead className="cursor-pointer select-none" onClick={() => toggleSort("country")}>
              {dict.leads.table.country}
              {sortIcon("country")}
            </TableHead>
            <TableHead className="cursor-pointer select-none" onClick={() => toggleSort("sector")}>
              {dict.leads.table.sector}
              {sortIcon("sector")}
            </TableHead>
            <TableHead className="cursor-pointer select-none" onClick={() => toggleSort("status")}>
              {dict.leads.table.status}
              {sortIcon("status")}
            </TableHead>
            <TableHead
              className="cursor-pointer select-none text-right"
              onClick={() => toggleSort("qualificationScore")}
            >
              {dict.leads.table.score}
              {sortIcon("qualificationScore")}
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sortedLeads.map((lead) => (
            <TableRow key={lead.id} data-state={selectedIds.has(lead.id) ? "selected" : undefined}>
              <TableCell>
                {lead.status === "PENDING_APPROVAL" && (
                  <Checkbox
                    checked={selectedIds.has(lead.id)}
                    onCheckedChange={() => toggleRow(lead.id)}
                    aria-label={dict.leads.bulk.selectRowLabel}
                  />
                )}
              </TableCell>
              <TableCell className="font-medium text-foreground">
                <Link href={`/dashboard/leads/${lead.id}`} className="hover:underline">
                  {lead.companyName}
                </Link>
              </TableCell>
              <TableCell className="text-muted-foreground">{lead.domain}</TableCell>
              <TableCell className="text-muted-foreground">{lead.country}</TableCell>
              <TableCell className="text-muted-foreground">{lead.sector}</TableCell>
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
    </div>
  );
}
