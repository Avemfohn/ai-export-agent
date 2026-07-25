import { Users } from "lucide-react";

import { LeadTable } from "@/components/leads/lead-table";
import { EmptyState } from "@/components/shared/empty-state";
import { getLeads } from "@/lib/api/leads";

export default async function LeadsPage() {
  let leads: Awaited<ReturnType<typeof getLeads>> = [];
  let errored = false;

  try {
    leads = await getLeads();
  } catch {
    errored = true;
  }

  if (errored) {
    return (
      <EmptyState
        icon={Users}
        title="Could not load leads"
        description="The backend API is unreachable. Make sure it is running at NEXT_PUBLIC_API_BASE_URL."
      />
    );
  }

  if (leads.length === 0) {
    return (
      <EmptyState
        icon={Users}
        title="No leads yet"
        description="Qualified buyer leads from the global supplier pool will appear here once scraping and AI filtering have run."
      />
    );
  }

  return <LeadTable leads={leads} />;
}
