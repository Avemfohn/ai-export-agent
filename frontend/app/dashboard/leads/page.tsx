import { Users } from "lucide-react";

import { LeadTable } from "@/components/leads/lead-table";
import { EmptyState } from "@/components/shared/empty-state";
import { getLeads } from "@/lib/api/leads";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

export default async function LeadsPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

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
        title={dict.leads.errorTitle}
        description={dict.leads.errorDescription}
      />
    );
  }

  if (leads.length === 0) {
    return (
      <EmptyState
        icon={Users}
        title={dict.leads.emptyTitle}
        description={dict.leads.emptyDescription}
      />
    );
  }

  return <LeadTable leads={leads} dict={dict} />;
}
