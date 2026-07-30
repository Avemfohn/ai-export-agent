// Mirrors backend/.../ai/client/EmailTemplatePlaceholders.java and the literal
// String.replace calls in MockAiClient.substitutePlaceholders.
//
// There is no shared schema between the two, so these must be changed
// together. Adding a token here that the backend doesn't substitute would tell
// the user a placeholder is supported when it would in fact survive verbatim
// into a real customer email.

export const SUPPORTED_PLACEHOLDERS = [
  "companyName",
  "contactFirstName",
  "senderName",
  "sector",
] as const;

export type SupportedPlaceholder = (typeof SUPPORTED_PLACEHOLDERS)[number];

/**
 * Matches `{{ name }}` with optional inner whitespace so we can warn about the
 * spaced form — the backend substitutes by exact literal match, so
 * `{{ companyName }}` is NOT replaced and ships as-is.
 */
const PLACEHOLDER_PATTERN = /\{\{(\s*)([A-Za-z0-9_]+)(\s*)\}\}/g;

export interface PlaceholderIssue {
  token: string;
  name: string;
  kind: "unknown" | "spaced";
}

/** Finds placeholders that will not substitute, so the editor can warn. */
export function findPlaceholderIssues(...texts: (string | undefined)[]): PlaceholderIssue[] {
  const issues = new Map<string, PlaceholderIssue>();

  for (const text of texts) {
    if (!text) continue;
    for (const match of text.matchAll(PLACEHOLDER_PATTERN)) {
      const [token, leading, name, trailing] = match;
      const known = (SUPPORTED_PLACEHOLDERS as readonly string[]).includes(name);
      if (!known) {
        issues.set(token, { token, name, kind: "unknown" });
      } else if (leading.length > 0 || trailing.length > 0) {
        issues.set(token, { token, name, kind: "spaced" });
      }
    }
  }

  return [...issues.values()];
}

/** Client-side mirror of the mock client's substitution, for the preview. */
export function substitutePlaceholders(
  text: string | undefined,
  values: Partial<Record<SupportedPlaceholder, string>>,
): string {
  if (!text) return "";
  let out = text;
  for (const name of SUPPORTED_PLACEHOLDERS) {
    // Unresolved values become empty, exactly as the backend does — the preview
    // should show the real gap rather than hide it behind invented copy.
    out = out.split(`{{${name}}}`).join(values[name] ?? "");
  }
  return out;
}
