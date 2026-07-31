"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { useTranslations } from "@/components/providers/i18n-provider";
import { ApiError } from "@/lib/api/client";
import { updateCampaignStatus } from "@/lib/api/campaigns";
import { ALLOWED_TRANSITIONS, type CampaignStatus } from "@/lib/types/campaign";

interface Props {
  campaignId: string;
  currentStatus: CampaignStatus;
  /** Approved leads sitting in this campaign — they stop/start with it. */
  affectedLeadCount: number;
}

/**
 * Status is changed through its own endpoint rather than the edit form, so a
 * one-click pause can't clobber a concurrent edit. Only legal transitions are
 * offered; the backend still enforces them (409).
 */
export function CampaignStatusControl({ campaignId, currentStatus, affectedLeadCount }: Props) {
  const { dict } = useTranslations();
  const router = useRouter();
  const [pending, setPending] = useState<CampaignStatus | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const options = ALLOWED_TRANSITIONS[currentStatus];

  async function apply(next: CampaignStatus) {
    setPending(next);
    setErrorMessage(null);
    try {
      await updateCampaignStatus(campaignId, next);
      router.refresh();
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : dict.campaigns.statusControl.error);
    } finally {
      setPending(null);
    }
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        {options.map((option) => (
          <Button
            key={option}
            type="button"
            variant="outline"
            size="sm"
            disabled={pending !== null}
            onClick={() => apply(option)}
          >
            {pending === option
              ? dict.campaigns.statusControl.applying
              : `${dict.campaigns.statusControl.moveTo} ${dict.campaigns.status[option]}`}
          </Button>
        ))}
      </div>
      {currentStatus === "ACTIVE" && affectedLeadCount > 0 && (
        <p className="text-xs text-muted-foreground">
          {affectedLeadCount} {dict.campaigns.statusControl.willStopWarning}
        </p>
      )}
      {currentStatus !== "ACTIVE" && affectedLeadCount > 0 && (
        <p className="text-xs text-destructive">
          {affectedLeadCount} {dict.campaigns.statusControl.currentlyBlocked}
        </p>
      )}
      {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}
    </div>
  );
}
