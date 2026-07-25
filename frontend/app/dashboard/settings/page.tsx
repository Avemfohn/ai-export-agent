import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

// NOTE: tenant_settings has no dedicated backend endpoint documented yet
// (Sprint 1, mock-data phase). This renders the expected shape read-only,
// matching the "buyer criteria" fields described in CLAUDE.md's AI-filtering
// step. Wire this up to a real GET /api/tenant-settings call once the
// backend exposes it.
const BUYER_CRITERIA_FIELDS = [
  { label: "Target Sectors", value: "Not configured" },
  { label: "Target Countries", value: "Not configured" },
  { label: "Minimum Company Size", value: "Not configured" },
  { label: "Excluded Keywords", value: "Not configured" },
  { label: "Preferred Languages", value: "Not configured" },
] as const;

export default function SettingsPage() {
  return (
    <div className="max-w-2xl space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Buyer Criteria</CardTitle>
          <CardDescription>
            Used by the AI filtering step to qualify scraped companies before
            they become leads. Read-only in this sprint.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {BUYER_CRITERIA_FIELDS.map((field) => (
            <div key={field.label} className="space-y-1">
              <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                {field.label}
              </label>
              <div className="rounded-md border border-input bg-muted/40 px-3 py-2 text-sm text-foreground">
                {field.value}
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
