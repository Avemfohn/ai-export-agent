"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Users,
  Megaphone,
  Send,
  MessageSquareText,
  Bell,
  Radar,
  Settings,
} from "lucide-react";

import { cn } from "@/lib/utils";
import { useTranslations } from "@/components/providers/i18n-provider";

function useNavItems() {
  const { dict } = useTranslations();
  return [
    { href: "/dashboard", label: dict.nav.overview, icon: LayoutDashboard },
    { href: "/dashboard/leads", label: dict.nav.leads, icon: Users },
    { href: "/dashboard/campaigns", label: dict.nav.campaigns, icon: Megaphone },
    { href: "/dashboard/outreach", label: dict.nav.outreach, icon: Send },
    { href: "/dashboard/responses", label: dict.nav.responses, icon: MessageSquareText },
    { href: "/dashboard/notifications", label: dict.nav.notifications, icon: Bell },
    { href: "/dashboard/scraping-jobs", label: dict.nav.scrapingJobs, icon: Radar },
    { href: "/dashboard/settings", label: dict.nav.settings, icon: Settings },
  ] as const;
}

export function Sidebar() {
  const pathname = usePathname();
  const { dict } = useTranslations();
  const navItems = useNavItems();

  return (
    <aside className="hidden w-64 shrink-0 flex-col border-r border-border bg-card md:flex">
      <div className="flex h-16 items-center gap-2 border-b border-border px-6">
        <Image
          src="/logo.png"
          alt={dict.app.name}
          width={2600}
          height={1418}
          priority
          className="h-8 w-auto shrink-0"
        />
        <span className="truncate text-sm font-semibold tracking-tight text-foreground">
          {dict.app.name}
        </span>
      </div>
      <nav className="flex flex-1 flex-col gap-1 p-3">
        {navItems.map((item) => {
          const isActive =
            item.href === "/dashboard"
              ? pathname === "/dashboard"
              : pathname === item.href || pathname.startsWith(`${item.href}/`);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
