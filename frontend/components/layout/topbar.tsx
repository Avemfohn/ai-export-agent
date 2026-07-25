"use client";

import { usePathname } from "next/navigation";

import { useTranslations } from "@/components/providers/i18n-provider";
import type { Dictionary } from "@/lib/i18n/dictionaries";

function resolveTitle(pathname: string, dict: Dictionary): string {
  const titles: Record<string, string> = {
    "/dashboard": dict.nav.overview,
    "/dashboard/leads": dict.nav.leads,
    "/dashboard/campaigns": dict.nav.campaigns,
    "/dashboard/outreach": dict.nav.outreach,
    "/dashboard/responses": dict.nav.responses,
    "/dashboard/notifications": dict.nav.notifications,
    "/dashboard/scraping-jobs": dict.nav.scrapingJobs,
    "/dashboard/settings": dict.nav.settings,
  };
  if (titles[pathname]) return titles[pathname];
  const segment = pathname.split("/").filter(Boolean).at(-2) ?? "";
  return titles[`/dashboard/${segment}`] ?? dict.topbar.dashboard;
}

export function Topbar() {
  const pathname = usePathname();
  const { dict } = useTranslations();
  const title = resolveTitle(pathname, dict);

  return (
    <header className="flex h-16 items-center border-b border-border bg-background px-6">
      <h1 className="text-lg font-semibold text-foreground">{title}</h1>
    </header>
  );
}
