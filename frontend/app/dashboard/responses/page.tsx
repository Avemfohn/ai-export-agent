import { MessageSquareText } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { getEmailResponses } from "@/lib/api/responses";
import type { ClassifiedIntent } from "@/lib/types/response";
import { getLocale } from "@/lib/i18n/get-locale";
import { getDictionary } from "@/lib/i18n/dictionaries";

const INTENT_VARIANT: Record<
  ClassifiedIntent,
  "success" | "warning" | "slate" | "destructive"
> = {
  INTERESTED: "success",
  NOT_INTERESTED: "destructive",
  NEEDS_INFO: "warning",
  OUT_OF_OFFICE: "slate",
  UNSUBSCRIBE: "destructive",
  SPAM: "destructive",
  UNKNOWN: "slate",
};

export default async function ResponsesPage() {
  const locale = await getLocale();
  const dict = getDictionary(locale);

  let responses: Awaited<ReturnType<typeof getEmailResponses>> = [];
  let errored = false;

  try {
    responses = await getEmailResponses();
  } catch {
    errored = true;
  }

  if (errored) {
    return (
      <EmptyState
        icon={MessageSquareText}
        title={dict.responses.errorTitle}
        description={dict.responses.errorDescription}
      />
    );
  }

  if (responses.length === 0) {
    return (
      <EmptyState
        icon={MessageSquareText}
        title={dict.responses.emptyTitle}
        description={dict.responses.emptyDescription}
      />
    );
  }

  return (
    <div className="space-y-3">
      {responses.map((response) => {
        const intent = response.classifiedIntent ?? "UNKNOWN";
        return (
          <Card key={response.id}>
            <CardContent className="flex items-start justify-between gap-4 p-4">
              <div className="min-w-0 space-y-1">
                <div className="flex items-center gap-2">
                  <p className="font-medium text-foreground">
                    {response.fromEmail ?? dict.responses.unknownSender}
                  </p>
                  <Badge variant={INTENT_VARIANT[intent]}>
                    {dict.responses.intent[intent]}
                  </Badge>
                </div>
                <p className="text-sm font-medium text-foreground">
                  {response.subject ?? dict.responses.noSubject}
                </p>
                <p className="truncate text-sm text-muted-foreground">
                  {response.body ?? ""}
                </p>
              </div>
              <p className="shrink-0 text-xs text-muted-foreground">
                {new Date(response.receivedAt).toLocaleString()}
              </p>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
