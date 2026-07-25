"use client";

import { usePathname } from "next/navigation";

const TITLES: Record<string, string> = {
  "/dashboard": "Overview",
  "/dashboard/leads": "Leads",
  "/dashboard/campaigns": "Campaigns",
  "/dashboard/outreach": "Outreach",
  "/dashboard/responses": "Responses",
  "/dashboard/notifications": "Notifications",
  "/dashboard/scraping-jobs": "Scraping Jobs",
  "/dashboard/settings": "Settings",
};

function resolveTitle(pathname: string): string {
  if (TITLES[pathname]) return TITLES[pathname];
  const segment = pathname.split("/").filter(Boolean).at(-2) ?? "";
  return TITLES[`/dashboard/${segment}`] ?? "Dashboard";
}

export function Topbar() {
  const pathname = usePathname();
  const title = resolveTitle(pathname);

  return (
    <header className="flex h-16 items-center border-b border-border bg-background px-6">
      <h1 className="text-lg font-semibold text-foreground">{title}</h1>
    </header>
  );
}
