"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { useTranslations } from "@/components/providers/i18n-provider";
import { requeueOutreachEmail } from "@/lib/api/outreach";

/**
 * Operator recovery action for a failed send. Deliberately manual — the
 * sending pipeline never retries on its own (see CLAUDE.md), so this button
 * is the only path from FAILED back onto the queue.
 */
export function RequeueButton({ emailId }: { emailId: string }) {
  const { dict } = useTranslations();
  const router = useRouter();
  const [status, setStatus] = useState<"idle" | "pending" | "error">("idle");

  async function handleRequeue() {
    setStatus("pending");
    try {
      await requeueOutreachEmail(emailId);
      setStatus("idle");
      router.refresh();
    } catch {
      setStatus("error");
    }
  }

  return (
    <div className="flex items-center justify-end gap-2">
      <Button
        variant="outline"
        size="sm"
        onClick={handleRequeue}
        disabled={status === "pending"}
      >
        {status === "pending"
          ? dict.outreach.requeue.pending
          : dict.outreach.requeue.button}
      </Button>
      {status === "error" && (
        <span className="text-xs text-destructive">
          {dict.outreach.requeue.error}
        </span>
      )}
    </div>
  );
}
