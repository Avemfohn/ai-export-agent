"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { bulkUpdateLeadStatus } from "@/lib/api/leads";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export function LeadBulkToolbar({
  selectedIds,
  dict,
  onDone,
}: {
  selectedIds: string[];
  dict: Dictionary;
  onDone: () => void;
}) {
  const router = useRouter();
  const [pending, setPending] = useState<"APPROVED" | "REJECTED" | null>(null);
  const [error, setError] = useState(false);

  async function run(status: "APPROVED" | "REJECTED") {
    setPending(status);
    setError(false);
    try {
      await bulkUpdateLeadStatus(selectedIds, status);
      onDone();
      router.refresh();
    } catch {
      setError(true);
    } finally {
      setPending(null);
    }
  }

  return (
    <div className="flex items-center gap-3 rounded-md border border-input bg-muted/40 px-4 py-2">
      <span className="text-sm font-medium">
        {selectedIds.length} {dict.leads.bulk.selectedSuffix}
      </span>
      <Button size="sm" disabled={pending !== null} onClick={() => run("APPROVED")}>
        {pending === "APPROVED" ? dict.leads.bulk.approving : dict.leads.bulk.approveSelected}
      </Button>
      <Button size="sm" variant="outline" disabled={pending !== null} onClick={() => run("REJECTED")}>
        {pending === "REJECTED" ? dict.leads.bulk.rejecting : dict.leads.bulk.rejectSelected}
      </Button>
      {error && <span className="text-sm text-destructive">{dict.leads.bulk.bulkActionError}</span>}
    </div>
  );
}
