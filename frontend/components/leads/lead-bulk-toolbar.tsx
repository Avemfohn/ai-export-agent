"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { bulkAssignLeadCampaign, bulkUpdateLeadStatus } from "@/lib/api/leads";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import type { TenantCampaign } from "@/lib/types/campaign";
import type { TenantLead } from "@/lib/types/lead";

const UNASSIGN_VALUE = "__none__";

export function LeadBulkToolbar({
  selectedLeads,
  campaigns,
  dict,
  onDone,
}: {
  selectedLeads: TenantLead[];
  campaigns: TenantCampaign[];
  dict: Dictionary;
  onDone: () => void;
}) {
  const router = useRouter();
  const [pending, setPending] = useState<string | null>(null);
  const [error, setError] = useState(false);

  // Selection now spans pending AND approved leads (approving one, assigning
  // the other), so each action reports how many of the selection it can
  // actually affect rather than silently no-op'ing on the rest.
  const approvableIds = selectedLeads
    .filter((lead) => lead.status === "PENDING_APPROVAL")
    .map((lead) => lead.id);
  const assignableIds = selectedLeads
    .filter((lead) => lead.status === "PENDING_APPROVAL" || lead.status === "APPROVED")
    .map((lead) => lead.id);

  // Archived/completed campaigns can't send, so offering them invites a dead end.
  const assignableCampaigns = campaigns
    .filter((c) => c.status !== "ARCHIVED" && c.status !== "COMPLETED")
    .sort((a, b) => a.name.localeCompare(b.name));

  async function run(action: string, fn: () => Promise<unknown>) {
    setPending(action);
    setError(false);
    try {
      await fn();
      onDone();
      router.refresh();
    } catch {
      setError(true);
    } finally {
      setPending(null);
    }
  }

  function eligibility(count: number) {
    return count === selectedLeads.length
      ? ""
      : ` (${count}/${selectedLeads.length})`;
  }

  return (
    <div className="flex flex-wrap items-center gap-3 rounded-md border border-input bg-muted/40 px-4 py-2">
      <span className="text-sm font-medium">
        {selectedLeads.length} {dict.leads.bulk.selectedSuffix}
      </span>

      <Button
        size="sm"
        disabled={pending !== null || approvableIds.length === 0}
        onClick={() => run("APPROVED", () => bulkUpdateLeadStatus(approvableIds, "APPROVED"))}
      >
        {pending === "APPROVED"
          ? dict.leads.bulk.approving
          : `${dict.leads.bulk.approveSelected}${eligibility(approvableIds.length)}`}
      </Button>

      <Button
        size="sm"
        variant="outline"
        disabled={pending !== null || approvableIds.length === 0}
        onClick={() => run("REJECTED", () => bulkUpdateLeadStatus(approvableIds, "REJECTED"))}
      >
        {pending === "REJECTED"
          ? dict.leads.bulk.rejecting
          : `${dict.leads.bulk.rejectSelected}${eligibility(approvableIds.length)}`}
      </Button>

      <Select
        disabled={pending !== null || assignableIds.length === 0}
        onValueChange={(value) =>
          run("CAMPAIGN", () =>
            bulkAssignLeadCampaign(
              assignableIds,
              value === UNASSIGN_VALUE ? null : value,
            ),
          )
        }
      >
        <SelectTrigger className="h-8 w-[220px] text-sm">
          <SelectValue
            placeholder={`${dict.leads.bulk.assignToCampaign}${eligibility(assignableIds.length)}`}
          />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={UNASSIGN_VALUE}>{dict.leads.bulk.removeFromCampaign}</SelectItem>
          {assignableCampaigns.map((campaign) => (
            <SelectItem key={campaign.id} value={campaign.id}>
              {campaign.name} · {dict.campaigns.status[campaign.status]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {error && <span className="text-sm text-destructive">{dict.leads.bulk.bulkActionError}</span>}
    </div>
  );
}
