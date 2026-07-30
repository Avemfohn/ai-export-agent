"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useTranslations } from "@/components/providers/i18n-provider";
import { ApiError } from "@/lib/api/client";
import { previewCriteria, updateTenantSettings } from "@/lib/api/tenant-settings";
import type {
  BuyerCriteria,
  ScoredSample,
} from "@/lib/types/tenant-settings";

/** Keys the guided form knows how to edit. Everything else is preserved untouched. */
const KEYWORDS_KEY = "keywords";
const MIN_REVENUE_KEY = "minAnnualRevenueUsd";

/** These are their own columns; duplicating them inside criteria confuses scoring. */
const DUPLICATED_KEYS = ["targetSectors", "targetRegions"];

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((v) => typeof v === "string");
}

function toCommaList(value: string[] | undefined): string {
  return (value ?? []).join(", ");
}

function fromCommaList(text: string): string[] {
  return text
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}

interface Props {
  initialCriteria: BuyerCriteria | null;
  initialSectors: string[] | null;
  initialRegions: string[] | null;
}

/**
 * Buyer criteria editor: guided inputs for the fields in common use, plus a
 * raw-JSON escape hatch for everything else (criteria are deliberately
 * schema-free, so the form can never cover every case).
 *
 * Two invariants make this safe:
 *
 *  1. **The parsed document is the single source of truth.** Guided inputs are
 *     lenses that spread over it, so keys the form knows nothing about — e.g.
 *     `importsFromTurkey` — survive an edit untouched. The backend replaces the
 *     document wholesale and never merges, which is what makes *deleting* a key
 *     in the raw editor actually work.
 *  2. **The raw editor is a mode, not a mirror.** Exactly one editor owns the
 *     document at a time. Continuous two-way sync would fight the user's cursor
 *     and reformat their JSON mid-keystroke.
 */
export function BuyerCriteriaCard({ initialCriteria, initialSectors, initialRegions }: Props) {
  const { dict } = useTranslations();
  const router = useRouter();

  const [doc, setDoc] = useState<BuyerCriteria>(initialCriteria ?? {});
  const [sectors, setSectors] = useState(toCommaList(initialSectors ?? undefined));
  const [regions, setRegions] = useState(toCommaList(initialRegions ?? undefined));

  // Non-null means the raw editor owns the document and must be applied first.
  const [rawText, setRawText] = useState<string | null>(null);
  const [rawError, setRawError] = useState<string | null>(null);

  const [status, setStatus] = useState<"idle" | "saving" | "saved" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [samples, setSamples] = useState<ScoredSample[] | null>(null);
  const [testing, setTesting] = useState(false);

  const rawMode = rawText !== null;
  const keywords = doc[KEYWORDS_KEY];
  const minRevenue = doc[MIN_REVENUE_KEY];

  // "Can't be edited here" rather than coercing — coercion would destroy data
  // the user put there deliberately via the raw editor.
  const keywordsEditable = keywords === undefined || isStringArray(keywords);
  const minRevenueEditable = minRevenue === undefined || typeof minRevenue === "number";

  const duplicatedKeys = DUPLICATED_KEYS.filter((k) => k in doc);

  function markDirty() {
    setStatus("idle");
    setErrorMessage(null);
  }

  // Writes are touch-triggered, never load-triggered: initialising a key on
  // mount would inject it into a tenant that never had it — schema drift caused
  // by merely opening the page.
  function setKey(key: string, value: unknown) {
    setDoc((prev) => ({ ...prev, [key]: value }));
    markDirty();
  }

  function openRaw() {
    setRawText(JSON.stringify(doc, null, 2));
    setRawError(null);
    markDirty();
  }

  function applyRaw() {
    try {
      const parsed = JSON.parse(rawText ?? "");
      if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
        setRawError(dict.settings.buyerCriteria.mustBeObject);
        return;
      }
      setDoc(parsed as BuyerCriteria);
      setRawText(null);
      setRawError(null);
      markDirty();
    } catch {
      setRawError(dict.settings.buyerCriteria.invalidJson);
    }
  }

  async function handleSave() {
    setStatus("saving");
    setErrorMessage(null);
    try {
      await updateTenantSettings({
        buyerCriteria: doc,
        targetSectors: fromCommaList(sectors),
        targetRegions: fromCommaList(regions),
      });
      setStatus("saved");
      // Postgres jsonb does not preserve key order or whitespace, so what the
      // server now holds can differ cosmetically from what's on screen.
      router.refresh();
    } catch (err) {
      setStatus("error");
      setErrorMessage(err instanceof ApiError ? err.message : null);
    }
  }

  async function handleTest() {
    setTesting(true);
    setSamples(null);
    setErrorMessage(null);
    try {
      const result = await previewCriteria({
        buyerCriteria: doc,
        targetSectors: fromCommaList(sectors),
        targetRegions: fromCommaList(regions),
      });
      setSamples(result.samples);
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : null);
      setStatus("error");
    } finally {
      setTesting(false);
    }
  }

  return (
    <div className="space-y-5">
      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.settings.buyerCriteria.keywords}
        </label>
        <Input
          disabled={rawMode || !keywordsEditable}
          value={isStringArray(keywords) ? toCommaList(keywords) : ""}
          onChange={(e) => setKey(KEYWORDS_KEY, fromCommaList(e.target.value))}
          placeholder={dict.settings.buyerCriteria.keywordsPlaceholder}
        />
        <p className="text-xs text-muted-foreground">
          {keywordsEditable
            ? dict.settings.buyerCriteria.keywordsHelp
            : dict.settings.buyerCriteria.unrepresentable}
        </p>
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.settings.buyerCriteria.minRevenue}
        </label>
        <Input
          type="number"
          min={0}
          disabled={rawMode || !minRevenueEditable}
          value={typeof minRevenue === "number" ? minRevenue : ""}
          onChange={(e) =>
            setKey(MIN_REVENUE_KEY, e.target.value === "" ? undefined : Number(e.target.value))
          }
          className="max-w-[220px]"
        />
        <p className="text-xs text-muted-foreground">
          {minRevenueEditable
            ? dict.settings.buyerCriteria.minRevenueHelp
            : dict.settings.buyerCriteria.unrepresentable}
        </p>
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.settings.buyerCriteria.targetSectors}
        </label>
        <Input
          disabled={rawMode}
          value={sectors}
          onChange={(e) => {
            setSectors(e.target.value);
            markDirty();
          }}
          placeholder={dict.settings.buyerCriteria.targetSectorsPlaceholder}
        />
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {dict.settings.buyerCriteria.targetRegions}
        </label>
        <Input
          disabled={rawMode}
          value={regions}
          onChange={(e) => {
            setRegions(e.target.value);
            markDirty();
          }}
          placeholder={dict.settings.buyerCriteria.targetRegionsPlaceholder}
        />
      </div>

      {duplicatedKeys.length > 0 && (
        <p className="rounded-md border border-input bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
          {dict.settings.buyerCriteria.duplicatedKeysWarning} {duplicatedKeys.join(", ")}
        </p>
      )}

      <div className="space-y-2 border-t border-border pt-4">
        {!rawMode ? (
          <Button type="button" variant="outline" size="sm" onClick={openRaw}>
            {dict.settings.buyerCriteria.showAdvanced}
          </Button>
        ) : (
          <div className="space-y-2">
            <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
              {dict.settings.buyerCriteria.advancedLabel}
            </label>
            <Textarea
              className="min-h-[220px] font-mono text-xs"
              value={rawText ?? ""}
              onChange={(e) => {
                setRawText(e.target.value);
                setRawError(null);
              }}
              spellCheck={false}
            />
            <p className="text-xs text-muted-foreground">
              {dict.settings.buyerCriteria.advancedHelp}
            </p>
            {rawError && <p className="text-xs text-destructive">{rawError}</p>}
            <div className="flex items-center gap-2">
              <Button type="button" size="sm" onClick={applyRaw}>
                {dict.settings.buyerCriteria.applyJson}
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => {
                  setRawText(null);
                  setRawError(null);
                }}
              >
                {dict.settings.buyerCriteria.cancelJson}
              </Button>
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3 border-t border-border pt-4">
        {/* Save stays disabled until raw JSON is applied — making Save implicitly
            apply would hide the moment the guided fields change under the user. */}
        <Button onClick={handleSave} disabled={status === "saving" || rawMode}>
          {status === "saving" ? dict.settings.buyerCriteria.saving : dict.settings.buyerCriteria.save}
        </Button>
        <Button type="button" variant="outline" onClick={handleTest} disabled={testing || rawMode}>
          {testing ? dict.settings.buyerCriteria.testing : dict.settings.buyerCriteria.testButton}
        </Button>
        {rawMode && (
          <span className="text-xs text-muted-foreground">
            {dict.settings.buyerCriteria.applyFirst}
          </span>
        )}
        {status === "saved" && (
          <span className="text-sm text-muted-foreground">{dict.settings.buyerCriteria.saved}</span>
        )}
        {status === "error" && (
          <span className="text-sm text-destructive">
            {errorMessage ?? dict.settings.buyerCriteria.saveError}
          </span>
        )}
      </div>

      {samples && (
        <div className="space-y-2 border-t border-border pt-4">
          <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {dict.settings.buyerCriteria.testResults}
          </p>
          <p className="text-xs text-muted-foreground">{dict.settings.buyerCriteria.testHelp}</p>
          {samples.length === 0 ? (
            <p className="text-sm text-muted-foreground">{dict.settings.buyerCriteria.testEmpty}</p>
          ) : (
            <ul className="space-y-2">
              {samples.map((sample) => (
                <li
                  key={sample.domain}
                  className="flex items-start justify-between gap-3 rounded-md border border-input bg-muted/40 px-3 py-2"
                >
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-foreground">{sample.companyName}</p>
                    <p className="text-xs text-muted-foreground">{sample.domain}</p>
                    {sample.rationale && (
                      <p className="mt-1 text-xs text-muted-foreground">{sample.rationale}</p>
                    )}
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-sm font-semibold text-foreground">{sample.score}</p>
                    <p className="text-xs text-muted-foreground">
                      {sample.wouldApprove
                        ? dict.settings.buyerCriteria.wouldApprove
                        : sample.wouldReject
                          ? dict.settings.buyerCriteria.wouldReject
                          : dict.settings.buyerCriteria.wouldReview}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
