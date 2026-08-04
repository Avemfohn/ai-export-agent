package com.aiexportagent.scraping.upload;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Works out which spreadsheet column feeds which field, from the header row.
 *
 * <p>The result is reported back to the user before anything is written. A
 * column that failed to match must be <em>visible</em>: silently ignoring the
 * one holding every website turns a good file into "0 suppliers created" with
 * no explanation.
 */
public final class ColumnDetector {

    private ColumnDetector() {
    }

    /**
     * @param indexByColumn resolved column - zero-based position in the sheet
     * @param unmatchedHeaders headers present in the file that matched nothing,
     *                         shown to the user so a missed mapping is obvious
     */
    public record Detection(
            Map<UploadColumn, Integer> indexByColumn,
            List<String> unmatchedHeaders
    ) {
        public boolean has(UploadColumn column) {
            return indexByColumn.containsKey(column);
        }

        public Integer indexOf(UploadColumn column) {
            return indexByColumn.get(column);
        }

        /** Required columns that were not found; empty means the file is usable. */
        public List<UploadColumn> missingRequired() {
            return java.util.Arrays.stream(UploadColumn.values())
                    .filter(UploadColumn::isRequired)
                    .filter(c -> !indexByColumn.containsKey(c))
                    .toList();
        }
    }

    /**
     * Match a header row against {@link UploadColumn} aliases.
     *
     * <p>First match wins per column: a sheet with both "Company" and "Company
     * Name" binds the leftmost. Ambiguity here is harmless — both hold the same
     * thing — and picking deterministically beats failing the upload.
     */
    public static Detection detect(List<String> headers) {
        Map<UploadColumn, Integer> resolved = new EnumMap<>(UploadColumn.class);
        List<String> unmatched = new java.util.ArrayList<>();

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header == null || header.isBlank()) continue;

            boolean matchedAny = false;
            for (UploadColumn column : UploadColumn.values()) {
                if (!resolved.containsKey(column) && column.matches(header)) {
                    resolved.put(column, i);
                    matchedAny = true;
                    break;
                }
            }
            if (!matchedAny) unmatched.add(header.trim());
        }

        return new Detection(Map.copyOf(resolved), List.copyOf(unmatched));
    }
}
