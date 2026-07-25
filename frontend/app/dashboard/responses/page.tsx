import { MessageSquareText } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { getEmailResponses } from "@/lib/api/responses";
import type { ClassifiedIntent } from "@/lib/types/response";

const INTENT_VARIANT: Record<
  ClassifiedIntent,
  "success" | "warning" | "slate" | "destructive"
> = {
  INTERESTED: "success",
  NOT_INTERESTED: "destructive",
  NEEDS_INFO: "warning",
  OUT_OF_OFFICE: "slate",
  UNSUBSCRIBE: "destructive",
  UNKNOWN: "slate",
};

export default async function ResponsesPage() {
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
        title="Could not load responses"
        description="The backend API is unreachable. Make sure it is running at NEXT_PUBLIC_API_BASE_URL."
      />
    );
  }

  if (responses.length === 0) {
    return (
      <EmptyState
        icon={MessageSquareText}
        title="No replies yet"
        description="Classified replies from leads — including warm replies — will appear here."
      />
    );
  }

  return (
    <div className="space-y-3">
      {responses.map((response) => (
        <Card key={response.id}>
          <CardContent className="flex items-start justify-between gap-4 p-4">
            <div className="min-w-0 space-y-1">
              <div className="flex items-center gap-2">
                <p className="font-medium text-foreground">
                  {response.fromEmail}
                </p>
                <Badge variant={INTENT_VARIANT[response.classifiedIntent]}>
                  {response.classifiedIntent.replace("_", " ")}
                </Badge>
              </div>
              <p className="text-sm font-medium text-foreground">
                {response.subject}
              </p>
              <p className="truncate text-sm text-muted-foreground">
                {response.snippet}
              </p>
            </div>
            <p className="shrink-0 text-xs text-muted-foreground">
              {new Date(response.receivedAt).toLocaleString()}
            </p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
