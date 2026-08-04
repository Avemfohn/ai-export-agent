package com.aiexportagent.scraping.upload;

import java.util.Locale;
import java.util.Set;

/**
 * Cleans individual spreadsheet cells before they reach the shared pool.
 *
 * <p>Static and Spring-free, same posture as {@link DomainNormalizer}.
 */
public final class CellSanitizer {

    private CellSanitizer() {
    }

    /**
     * Leading characters that make a spreadsheet treat a cell as a formula.
     * Inert in Postgres; executable the moment the value lands back in Excel.
     */
    private static final Set<Character> FORMULA_TRIGGERS = Set.of('=', '+', '-', '@', '\t', '\r');

    private static final Set<String> PLACEHOLDERS = Set.of(
            "n/a", "na", "-", "--", "none", "null", "nil", "tbd", "tba",
            "unknown", "yok", "bilinmiyor", "?");

    /**
     * Strip control characters, collapse whitespace, neutralise formula
     * triggers, and truncate to the destination column width.
     *
     * <p>Neutralising happens here, at ingest, rather than at every egress
     * point. This data flows into AI-drafted email bodies and any future CSV
     * export; one rule at the boundary is far more reliable than remembering to
     * escape at each place it is emitted. Control characters must go for a
     * second reason: Postgres rejects NUL in text columns, and it would surface
     * as an opaque 500 rather than a skipped row.
     *
     * @return the cleaned value, or null if nothing usable remained
     */
    public static String clean(String raw, int maxLength) {
        if (raw == null) return null;

        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            // Turn any control character into a space so the words either side
            // do not weld together; keep everything else, spaces included.
            sb.append(Character.isISOControl(c) ? ' ' : c);
        }

        String value = sb.toString().replaceAll("\\s+", " ").trim();
        if (value.isEmpty()) return null;
        if (PLACEHOLDERS.contains(value.toLowerCase(Locale.ROOT))) return null;

        // Prefix rather than delete: the original text stays readable and
        // verifiable, it simply stops being a formula.
        if (FORMULA_TRIGGERS.contains(value.charAt(0))) {
            value = "'" + value;
        }

        if (value.length() > maxLength) {
            value = value.substring(0, maxLength).trim();
        }
        return value.isEmpty() ? null : value;
    }
}
