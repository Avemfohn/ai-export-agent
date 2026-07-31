"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useTranslations } from "@/components/providers/i18n-provider";
import { EmailTemplateFields } from "@/components/settings/email-template-fields";
import { ApiError } from "@/lib/api/client";
import { createCampaign, updateCampaign } from "@/lib/api/campaigns";
import type { TenantCampaign } from "@/lib/types/campaign";

interface Props {
  /** Absent = create mode. */
  campaign?: TenantCampaign;
  senderName: string | null;
}

/**
 * One form for both creating and editing.
 *
 * On create the template isn't sent at all — the backend seeds it from the
 * tenant's current default, which is what makes a new campaign immediately
 * usable. On edit it's a full replacement (PUT), so every field goes every
 * time; that's deliberate, because `description` is nullable and a partial
 * update couldn't express "clear it".
 */
export function CampaignForm({ campaign, senderName }: Props) {
  const { dict } = useTranslations();
  const router = useRouter();

  const isEdit = campaign !== undefined;
  const template = campaign?.emailDraftTemplateSnapshot ?? {};

  const [name, setName] = useState(campaign?.name ?? "");
  const [description, setDescription] = useState(campaign?.description ?? "");
  const [activateNow, setActivateNow] = useState(true);
  const [subject, setSubject] = useState(template.subject ?? "");
  const [body, setBody] = useState(template.body ?? "");
  const [notes, setNotes] = useState(template.notes ?? "");

  const [status, setStatus] = useState<"idle" | "saving" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const missingName = name.trim().length === 0;
  // On edit the template is being replaced, so it must be valid. On create it
  // isn't sent, so blank fields are fine — the tenant default is copied.
  const missingTemplate = isEdit && (subject.trim().length === 0 || body.trim().length === 0);

  function markDirty() {
    setStatus("idle");
    setErrorMessage(null);
  }

  async function handleSubmit() {
    setStatus("saving");
    setErrorMessage(null);
    try {
      if (isEdit) {
        await updateCampaign(campaign.id, {
          name,
          description: description.trim() === "" ? null : description,
          emailDraftTemplate: { ...template, subject, body, notes },
        });
        router.push(`/dashboard/campaigns/${campaign.id}`);
      } else {
        const created = await createCampaign({
          name,
          description: description.trim() === "" ? null : description,
          status: activateNow ? "ACTIVE" : "DRAFT",
        });
        router.push(`/dashboard/campaigns/${created.id}`);
      }
      router.refresh();
    } catch (err) {
      setStatus("error");
      setErrorMessage(err instanceof ApiError ? err.message : null);
    }
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.campaigns.form.name}
        </label>
        <Input
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            markDirty();
          }}
          placeholder={dict.campaigns.form.namePlaceholder}
        />
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.campaigns.form.description}
        </label>
        <Textarea
          className="min-h-[80px]"
          value={description}
          onChange={(e) => {
            setDescription(e.target.value);
            markDirty();
          }}
          placeholder={dict.campaigns.form.descriptionPlaceholder}
        />
      </div>

      {!isEdit && (
        <div className="space-y-1">
          <label className="flex items-center gap-2 text-sm font-medium">
            <Checkbox
              checked={activateNow}
              onCheckedChange={(checked) => {
                setActivateNow(checked === true);
                markDirty();
              }}
            />
            {dict.campaigns.form.activateNow}
          </label>
          <p className="text-xs text-muted-foreground">{dict.campaigns.form.activateNowHelp}</p>
        </div>
      )}

      {isEdit ? (
        <div className="space-y-2 border-t border-border pt-5">
          <p className="text-sm font-medium text-foreground">{dict.campaigns.form.templateTitle}</p>
          <p className="text-xs text-muted-foreground">{dict.campaigns.form.templateHelp}</p>
          <EmailTemplateFields
            subject={subject}
            body={body}
            notes={notes}
            onSubjectChange={(v) => {
              setSubject(v);
              markDirty();
            }}
            onBodyChange={(v) => {
              setBody(v);
              markDirty();
            }}
            onNotesChange={(v) => {
              setNotes(v);
              markDirty();
            }}
            senderName={senderName}
            labels={dict.settings.emailTemplate}
          />
        </div>
      ) : (
        <p className="rounded-md border border-input bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
          {dict.campaigns.form.templateCopiedNote}
        </p>
      )}

      <div className="flex flex-wrap items-center gap-3 border-t border-border pt-4">
        <Button onClick={handleSubmit} disabled={status === "saving" || missingName || missingTemplate}>
          {status === "saving"
            ? dict.campaigns.form.saving
            : isEdit
              ? dict.campaigns.form.save
              : dict.campaigns.form.create}
        </Button>
        <Button type="button" variant="ghost" onClick={() => router.back()}>
          {dict.campaigns.form.cancel}
        </Button>
        {missingName && (
          <span className="text-xs text-destructive">{dict.campaigns.form.nameRequired}</span>
        )}
        {missingTemplate && (
          <span className="text-xs text-destructive">
            {dict.settings.emailTemplate.requiredFields}
          </span>
        )}
        {status === "error" && (
          <span className="text-sm text-destructive">
            {errorMessage ?? dict.campaigns.form.saveError}
          </span>
        )}
      </div>
    </div>
  );
}
