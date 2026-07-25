import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ThemeToggle } from "@/components/settings/theme-toggle";
import { LanguageSwitcher } from "@/components/settings/language-switcher";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

// NOTE: tenant_settings has no dedicated backend endpoint documented yet
// (Sprint 1, mock-data phase). This renders the expected shape read-only,
// matching the "buyer criteria" fields described in CLAUDE.md's AI-filtering
// step. Wire this up to a real GET /api/tenant-settings call once the
// backend exposes it.

export default async function SettingsPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  const buyerCriteriaFields = [
    dict.settings.buyerCriteria.targetSectors,
    dict.settings.buyerCriteria.targetCountries,
    dict.settings.buyerCriteria.minCompanySize,
    dict.settings.buyerCriteria.excludedKeywords,
    dict.settings.buyerCriteria.preferredLanguages,
  ];

  return (
    <div className="max-w-2xl space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>{dict.settings.appearance.title}</CardTitle>
          <CardDescription>{dict.settings.appearance.description}</CardDescription>
        </CardHeader>
        <CardContent>
          <ThemeToggle />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{dict.settings.language.title}</CardTitle>
          <CardDescription>{dict.settings.language.description}</CardDescription>
        </CardHeader>
        <CardContent>
          <LanguageSwitcher />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{dict.settings.buyerCriteria.title}</CardTitle>
          <CardDescription>
            {dict.settings.buyerCriteria.description}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {buyerCriteriaFields.map((label) => (
            <div key={label} className="space-y-1">
              <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                {label}
              </label>
              <div className="rounded-md border border-input bg-muted/40 px-3 py-2 text-sm text-foreground">
                {dict.settings.buyerCriteria.notConfigured}
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
