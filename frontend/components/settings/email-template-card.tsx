"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { useTranslations } from "@/components/providers/i18n-provider";
import { EmailTemplateFields } from "@/components/settings/email-template-fields";
import { ApiError } from "@/lib/api/client";
import { updateTenantSettings } from "@/lib/api/tenant-settings";
import type { EmailDraftTemplate } from "@/lib/types/tenant-settings";

interface Props {
  initialTemplate: EmailDraftTemplate | null;
  senderName: string | null;
  queuedCount: number;
}

/**
 * The tenant's default outreach template. Thin wrapper around
 * {@link EmailTemplateFields} — this component owns only persistence; the
 * campaign editor reuses the same fields with its own save path.
 *
 * Subject and body are a hard requirement of the drafting code (a missing one
 * becomes an empty string and would send a blank email), so the save button is
 * gated on them. Unknown keys in the stored template are preserved by the
 * spread in `handleSave`.
 */
export function EmailTemplateCard({ initialTemplate, senderName, queuedCount }: Props) {
  const { dict } = useTranslations();
  const router = useRouter();

  const template = initialTemplate ?? {};
  const [subject, setSubject] = useState(template.subject ?? "");
  const [body, setBody] = useState(template.body ?? "");
  const [notes, setNotes] = useState(template.notes ?? "");

  const [status, setStatus] = useState<"idle" | "saving" | "saved" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Mirrors the backend rule so the user gets translated feedback without a
  // round-trip; the backend remains the authority.
  const missingRequired = subject.trim().length === 0 || body.trim().length === 0;

  function markDirty() {
    setStatus("idle");
    setErrorMessage(null);
  }

  async function handleSave() {
    setStatus("saving");
    setErrorMessage(null);
    try {
      await updateTenantSettings({
        // Spread first so keys we don't edit here survive the round-trip.
        emailDraftTemplate: { ...template, subject, body, notes },
      });
      setStatus("saved");
      router.refresh();
    } catch (err) {
      setStatus("error");
      setErrorMessage(err instanceof ApiError ? err.message : null);
    }
  }

  return (
    <div className="space-y-5">
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

      {queuedCount > 0 && (
        <p className="rounded-md border border-input bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
          {queuedCount} {dict.settings.emailTemplate.queuedWarning}
        </p>
      )}

      <p className="text-xs text-muted-foreground">{dict.settings.emailTemplate.campaignNote}</p>

      <div className="flex flex-wrap items-center gap-3 border-t border-border pt-4">
        <Button onClick={handleSave} disabled={status === "saving" || missingRequired}>
          {status === "saving" ? dict.settings.emailTemplate.saving : dict.settings.emailTemplate.save}
        </Button>
        {missingRequired && (
          <span className="text-xs text-destructive">{dict.settings.emailTemplate.requiredFields}</span>
        )}
        {status === "saved" && (
          <span className="text-sm text-muted-foreground">{dict.settings.emailTemplate.saved}</span>
        )}
        {status === "error" && (
          <span className="text-sm text-destructive">
            {errorMessage ?? dict.settings.emailTemplate.saveError}
          </span>
        )}
      </div>
    </div>
  );
}
