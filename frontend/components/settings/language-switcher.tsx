"use client";

import { cn } from "@/lib/utils";
import { useTranslations } from "@/components/providers/i18n-provider";
import type { Locale } from "@/lib/i18n/dictionaries";

const OPTIONS: { value: Locale; labelKey: "english" | "turkish" }[] = [
  { value: "en", labelKey: "english" },
  { value: "tr", labelKey: "turkish" },
];

export function LanguageSwitcher() {
  const { locale, dict, setLocale } = useTranslations();

  return (
    <div className="inline-flex rounded-md border border-input p-1">
      {OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() => setLocale(option.value)}
          aria-pressed={locale === option.value}
          className={cn(
            "rounded px-3 py-1.5 text-sm font-medium transition-colors",
            locale === option.value
              ? "bg-primary text-primary-foreground"
              : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
          )}
        >
          {dict.settings.language[option.labelKey]}
        </button>
      ))}
    </div>
  );
}
