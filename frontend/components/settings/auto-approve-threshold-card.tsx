"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { useTranslations } from "@/components/providers/i18n-provider";
import { updateAutoApproveThreshold } from "@/lib/api/tenant-settings";

const DEFAULT_THRESHOLD = 80;

export function AutoApproveThresholdCard({ initialThreshold }: { initialThreshold: number | null }) {
  const { dict } = useTranslations();
  const [enabled, setEnabled] = useState(initialThreshold !== null);
  const [value, setValue] = useState(initialThreshold ?? DEFAULT_THRESHOLD);
  const [status, setStatus] = useState<"idle" | "saving" | "saved" | "error">("idle");

  async function handleSave() {
    setStatus("saving");
    try {
      await updateAutoApproveThreshold(enabled ? value : null);
      setStatus("saved");
    } catch {
      setStatus("error");
    }
  }

  return (
    <div className="space-y-4">
      <label className="flex items-center gap-2 text-sm font-medium">
        <Checkbox
          checked={enabled}
          onCheckedChange={(checked) => {
            setEnabled(checked === true);
            setStatus("idle");
          }}
        />
        {dict.settings.autoApprove.enableLabel}
      </label>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.settings.autoApprove.thresholdLabel}
        </label>
        <Input
          type="number"
          min={0}
          max={100}
          step="0.01"
          disabled={!enabled}
          value={value}
          onChange={(e) => {
            setValue(Number(e.target.value));
            setStatus("idle");
          }}
          className="max-w-[160px]"
        />
        <p className="text-xs text-muted-foreground">{dict.settings.autoApprove.thresholdHelp}</p>
      </div>

      <div className="flex items-center gap-3">
        <Button onClick={handleSave} disabled={status === "saving"}>
          {status === "saving" ? dict.settings.autoApprove.savingLabel : dict.settings.autoApprove.saveButton}
        </Button>
        {status === "saved" && <span className="text-sm text-muted-foreground">{dict.settings.autoApprove.savedLabel}</span>}
        {status === "error" && <span className="text-sm text-destructive">{dict.settings.autoApprove.errorLabel}</span>}
      </div>
    </div>
  );
}
