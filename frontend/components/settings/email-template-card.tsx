"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useTranslations } from "@/components/providers/i18n-provider";
import { ApiError } from "@/lib/api/client";
import { updateTenantSettings } from "@/lib/api/tenant-settings";
import type { EmailDraftTemplate } from "@/lib/types/tenant-settings";
import {
  SUPPORTED_PLACEHOLDERS,
  findPlaceholderIssues,
  substitutePlaceholders,
} from "@/lib/email-template-placeholders";

/** Stand-in recipient for the preview; senderName comes from real settings. */
const SAMPLE = {
  companyName: "Meadowbrook Home Co",
  contactFirstName: "Sarah",
  sector: "home textiles",
};

interface Props {
  initialTemplate: EmailDraftTemplate | null;
  senderName: string | null;
  queuedCount: number;
}

/**
 * Structured editor for the email draft template. The stored value is
 * free-form JSON, but `subject` and `body` are a hard requirement of the
 * drafting code (a missing one becomes an empty string and would send a blank
 * email), so they get real inputs rather than a JSON blob.
 *
 * Unknown keys in the stored template are preserved on save — the spread in
 * `handleSave` is what guarantees that.
 */
export function EmailTemplateCard({ initialTemplate, senderName, queuedCount }: Props) {
  const { dict } = useTranslations();
  const router = useRouter();

  const template = initialTemplate ?? {};
  const [subject, setSubject] = useState(template.subject ?? "");
  const [body, setBody] = useState(template.body ?? "");
  const [notes, setNotes] = useState(template.notes ?? "");
  const [showPreview, setShowPreview] = useState(false);

  const [status, setStatus] = useState<"idle" | "saving" | "saved" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const issues = findPlaceholderIssues(subject, body);
  // Mirrors the backend rule so the user gets translated feedback before a
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

  const previewValues = {
    ...SAMPLE,
    senderName: senderName ?? "",
  };

  return (
    <div className="space-y-5">
      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.settings.emailTemplate.subject}
        </label>
        <Input
          value={subject}
          onChange={(e) => {
            setSubject(e.target.value);
            markDirty();
          }}
          placeholder={dict.settings.emailTemplate.subjectPlaceholder}
        />
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.settings.emailTemplate.body}
        </label>
        <Textarea
          className="min-h-[220px]"
          value={body}
          onChange={(e) => {
            setBody(e.target.value);
            markDirty();
          }}
          placeholder={dict.settings.emailTemplate.bodyPlaceholder}
        />
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.settings.emailTemplate.notes}
        </label>
        <Textarea
          className="min-h-[100px]"
          value={notes}
          onChange={(e) => {
            setNotes(e.target.value);
            markDirty();
          }}
          placeholder={dict.settings.emailTemplate.notesPlaceholder}
        />
        <p className="text-xs text-muted-foreground">{dict.settings.emailTemplate.notesHelp}</p>
      </div>

      <div className="rounded-md border border-input bg-muted/40 px-3 py-2">
        <p className="text-xs font-medium text-foreground">
          {dict.settings.emailTemplate.placeholdersLabel}
        </p>
        <p className="mt-1 font-mono text-xs text-muted-foreground">
          {SUPPORTED_PLACEHOLDERS.map((p) => `{{${p}}}`).join("  ")}
        </p>
      </div>

      {issues.length > 0 && (
        <div className="space-y-1 rounded-md border border-input bg-muted/40 px-3 py-2">
          {issues.map((issue) => (
            <p key={issue.token} className="text-xs text-destructive">
              <span className="font-mono">{issue.token}</span>{" "}
              {issue.kind === "spaced"
                ? dict.settings.emailTemplate.spacedPlaceholder
                : dict.settings.emailTemplate.unknownPlaceholder}
            </p>
          ))}
        </div>
      )}

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
        <Button type="button" variant="outline" onClick={() => setShowPreview((v) => !v)}>
          {showPreview ? dict.settings.emailTemplate.hidePreview : dict.settings.emailTemplate.showPreview}
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

      {showPreview && (
        <div className="space-y-2 border-t border-border pt-4">
          <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {dict.settings.emailTemplate.previewLabel}
          </p>
          <div className="rounded-md border border-input bg-muted/40 px-3 py-2">
            <p className="text-sm font-medium text-foreground">
              {substitutePlaceholders(subject, previewValues)}
            </p>
            <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">
              {substitutePlaceholders(body, previewValues)}
            </p>
          </div>
          <p className="text-xs text-muted-foreground">{dict.settings.emailTemplate.previewHelp}</p>
        </div>
      )}
    </div>
  );
}
