"use client";

import { useEffect, useState } from "react";
import { useTheme } from "next-themes";
import { Moon, Sun, Laptop } from "lucide-react";

import { cn } from "@/lib/utils";
import { useTranslations } from "@/components/providers/i18n-provider";

const OPTIONS = [
  { value: "light", icon: Sun },
  { value: "dark", icon: Moon },
  { value: "system", icon: Laptop },
] as const;

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const { dict } = useTranslations();
  // next-themes only knows the resolved theme after mount (it reads
  // localStorage/matchMedia client-side) — render a stable placeholder
  // until then to avoid a hydration mismatch.
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  const labels: Record<(typeof OPTIONS)[number]["value"], string> = {
    light: dict.settings.appearance.light,
    dark: dict.settings.appearance.dark,
    system: dict.settings.appearance.system,
  };

  return (
    <div className="inline-flex rounded-md border border-input p-1">
      {OPTIONS.map((option) => {
        const Icon = option.icon;
        const isActive = mounted && theme === option.value;
        return (
          <button
            key={option.value}
            type="button"
            onClick={() => setTheme(option.value)}
            aria-pressed={isActive}
            className={cn(
              "flex items-center gap-2 rounded px-3 py-1.5 text-sm font-medium transition-colors",
              isActive
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
            )}
          >
            <Icon className="h-4 w-4" />
            {labels[option.value]}
          </button>
        );
      })}
    </div>
  );
}
