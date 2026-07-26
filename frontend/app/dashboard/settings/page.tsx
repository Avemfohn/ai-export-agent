import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ThemeToggle } from "@/components/settings/theme-toggle";
import { LanguageSwitcher } from "@/components/settings/language-switcher";
import { AutoApproveThresholdCard } from "@/components/settings/auto-approve-threshold-card";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";
import { getTenantSettings } from "@/lib/api/tenant-settings";

// NOTE: buyer_criteria (JSONB) still has no dedicated update endpoint
// (Sprint 1, mock-data phase) — that card below stays read-only. The
// auto-approve threshold is the first real tenant_settings mutation, wired
// to GET/PATCH /api/tenant-settings.

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

  let autoApproveThreshold: number | null = null;
  try {
    // Backend uses default-property-inclusion: non_null (application.yml),
    // so a null threshold is OMITTED from the JSON, not sent as `null` —
    // normalize to null here so `initialThreshold !== null` downstream works.
    autoApproveThreshold = (await getTenantSettings()).autoApproveThreshold ?? null;
  } catch {
    // Fall through with null — the card below is still usable (defaults to
    // "off"), just can't reflect a saved value if the backend is unreachable.
  }

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
          <CardTitle>{dict.settings.autoApprove.title}</CardTitle>
          <CardDescription>{dict.settings.autoApprove.description}</CardDescription>
        </CardHeader>
        <CardContent>
          <AutoApproveThresholdCard initialThreshold={autoApproveThreshold} />
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
