"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import type { Dictionary } from "@/lib/i18n/dictionaries";
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

/**
 * Labels are passed in rather than read from a fixed dictionary path, so the
 * same fields serve both tenant settings and a campaign override. Typing this
 * as the settings slice (rather than a namespace string) makes TypeScript
 * enforce that every key exists — which is what keeps the two dictionaries in
 * step.
 */
export type EmailTemplateLabels = Dictionary["settings"]["emailTemplate"];

interface Props {
  subject: string;
  body: string;
  notes: string;
  onSubjectChange: (value: string) => void;
  onBodyChange: (value: string) => void;
  onNotesChange: (value: string) => void;
  senderName: string | null;
  labels: EmailTemplateLabels;
}

/**
 * Fully controlled subject/body/notes inputs, placeholder warnings and preview.
 *
 * <p>Deliberately owns no save button, no API call and no save state: a
 * campaign's template is saved as part of creating the campaign, so there is
 * nothing to save against until the campaign exists. Keeping persistence in the
 * parent is what lets `/campaigns/new` and `/campaigns/[id]/edit` use exactly
 * the same component as the settings page.
 */
export function EmailTemplateFields({
  subject,
  body,
  notes,
  onSubjectChange,
  onBodyChange,
  onNotesChange,
  senderName,
  labels,
}: Props) {
  const [showPreview, setShowPreview] = useState(false);
  const issues = findPlaceholderIssues(subject, body);

  const previewValues = { ...SAMPLE, senderName: senderName ?? "" };

  return (
    <div className="space-y-5">
      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {labels.subject}
        </label>
        <Input
          value={subject}
          onChange={(e) => onSubjectChange(e.target.value)}
          placeholder={labels.subjectPlaceholder}
        />
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {labels.body}
        </label>
        <Textarea
          className="min-h-[220px]"
          value={body}
          onChange={(e) => onBodyChange(e.target.value)}
          placeholder={labels.bodyPlaceholder}
        />
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {labels.notes}
        </label>
        <Textarea
          className="min-h-[100px]"
          value={notes}
          onChange={(e) => onNotesChange(e.target.value)}
          placeholder={labels.notesPlaceholder}
        />
        <p className="text-xs text-muted-foreground">{labels.notesHelp}</p>
      </div>

      <div className="rounded-md border border-input bg-muted/40 px-3 py-2">
        <p className="text-xs font-medium text-foreground">{labels.placeholdersLabel}</p>
        <p className="mt-1 font-mono text-xs text-muted-foreground">
          {SUPPORTED_PLACEHOLDERS.map((p) => `{{${p}}}`).join("  ")}
        </p>
      </div>

      {issues.length > 0 && (
        <div className="space-y-1 rounded-md border border-input bg-muted/40 px-3 py-2">
          {issues.map((issue) => (
            <p key={issue.token} className="text-xs text-destructive">
              <span className="font-mono">{issue.token}</span>{" "}
              {issue.kind === "spaced" ? labels.spacedPlaceholder : labels.unknownPlaceholder}
            </p>
          ))}
        </div>
      )}

      <div>
        <Button type="button" variant="outline" size="sm" onClick={() => setShowPreview((v) => !v)}>
          {showPreview ? labels.hidePreview : labels.showPreview}
        </Button>
        {showPreview && (
          <div className="mt-3 space-y-2">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
              {labels.previewLabel}
            </p>
            <div className="rounded-md border border-input bg-muted/40 px-3 py-2">
              <p className="text-sm font-medium text-foreground">
                {substitutePlaceholders(subject, previewValues)}
              </p>
              <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">
                {substitutePlaceholders(body, previewValues)}
              </p>
            </div>
            <p className="text-xs text-muted-foreground">{labels.previewHelp}</p>
          </div>
        )}
      </div>
    </div>
  );
}
