import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CampaignForm } from "@/components/campaigns/campaign-form";
import { getTenantSettings } from "@/lib/api/tenant-settings";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

export default async function NewCampaignPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  // Only used to render a realistic preview signature; the template itself is
  // copied server-side from the tenant default when the campaign is created.
  let senderName: string | null = null;
  try {
    senderName = (await getTenantSettings()).emailSenderName ?? null;
  } catch {
    // Non-critical — the preview just shows an unsigned email.
  }

  return (
    <div className="max-w-3xl">
      <Card>
        <CardHeader>
          <CardTitle>{dict.campaigns.form.createTitle}</CardTitle>
          <CardDescription>{dict.campaigns.form.createDescription}</CardDescription>
        </CardHeader>
        <CardContent>
          <CampaignForm senderName={senderName} />
        </CardContent>
      </Card>
    </div>
  );
}
