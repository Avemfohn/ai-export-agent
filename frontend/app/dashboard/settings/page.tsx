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
import { BuyerCriteriaCard } from "@/components/settings/buyer-criteria-card";
import { EmailTemplateCard } from "@/components/settings/email-template-card";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";
import { getTenantSettings } from "@/lib/api/tenant-settings";
import { getOutreachEmails } from "@/lib/api/outreach";
import type { TenantSettingsResponse } from "@/lib/types/tenant-settings";

export default async function SettingsPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  // The backend uses default-property-inclusion: non_null (application.yml), so
  // unset fields are ABSENT rather than null — normalize with `?? null` and
  // never compare against null upstream of that.
  let settings: TenantSettingsResponse = {};
  let settingsErrored = false;
  try {
    settings = await getTenantSettings();
  } catch {
    // Surfaced in the cards below rather than swallowed — rendering empty
    // editors on a failed load would look like "you have no configuration".
    settingsErrored = true;
  }

  // Editing the template does not affect emails already rendered and queued, so
  // show how many are in flight. Counted from the existing list endpoint rather
  // than adding an endpoint for a single number.
  let queuedCount = 0;
  try {
    queuedCount = (await getOutreachEmails()).filter((e) => e.status === "QUEUED").length;
  } catch {
    // Non-critical — the warning just won't render.
  }

  return (
    <div className="max-w-3xl space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>{dict.settings.buyerCriteria.title}</CardTitle>
          <CardDescription>{dict.settings.buyerCriteria.description}</CardDescription>
        </CardHeader>
        <CardContent>
          {settingsErrored ? (
            <p className="text-sm text-muted-foreground">{dict.settings.loadError}</p>
          ) : (
            <BuyerCriteriaCard
              initialCriteria={settings.buyerCriteria ?? null}
              initialSectors={settings.targetSectors ?? null}
              initialRegions={settings.targetRegions ?? null}
            />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{dict.settings.emailTemplate.title}</CardTitle>
          <CardDescription>{dict.settings.emailTemplate.description}</CardDescription>
        </CardHeader>
        <CardContent>
          {settingsErrored ? (
            <p className="text-sm text-muted-foreground">{dict.settings.loadError}</p>
          ) : (
            <EmailTemplateCard
              initialTemplate={settings.emailDraftTemplate ?? null}
              senderName={settings.emailSenderName ?? null}
              queuedCount={queuedCount}
            />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{dict.settings.autoApprove.title}</CardTitle>
          <CardDescription>{dict.settings.autoApprove.description}</CardDescription>
        </CardHeader>
        <CardContent>
          <AutoApproveThresholdCard initialThreshold={settings.autoApproveThreshold ?? null} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{dict.settings.sendingIdentity.title}</CardTitle>
          <CardDescription>{dict.settings.sendingIdentity.description}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
              {dict.settings.sendingIdentity.senderName}
            </label>
            <div className="rounded-md border border-input bg-muted/40 px-3 py-2 text-sm text-foreground">
              {settings.emailSenderName || dict.settings.sendingIdentity.notConfigured}
            </div>
          </div>
          <div className="space-y-1">
            <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
              {dict.settings.sendingIdentity.senderAddress}
            </label>
            <div className="rounded-md border border-input bg-muted/40 px-3 py-2 text-sm text-foreground">
              {settings.emailSenderAddress || dict.settings.sendingIdentity.notConfigured}
            </div>
          </div>
        </CardContent>
      </Card>

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
    </div>
  );
}
