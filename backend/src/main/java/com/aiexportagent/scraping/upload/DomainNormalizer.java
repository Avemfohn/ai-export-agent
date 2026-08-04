package com.aiexportagent.scraping.upload;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Reduces whatever a spreadsheet calls a website into the canonical domain used
 * as the shared pool's dedup key.
 *
 * <p>This is the load-bearing piece of trade-fair ingestion. {@code
 * global_suppliers.domain} carries a UNIQUE constraint, and that constraint is
 * the only thing preventing the pool from filling with the same company over
 * and over. If {@code example.com} and {@code www.example.com} normalise
 * differently, the constraint never fires and the pool quietly rots — the exact
 * failure the shared pool exists to prevent.
 *
 * <p>Static and Spring-free so it can be unit-tested directly, mirroring
 * {@code common.validation.SettingsJsonValidator}.
 */
public final class DomainNormalizer {

    private DomainNormalizer() {
    }

    /**
     * Spreadsheet cells that mean "no website" rather than naming one. Without
     * this the pool acquires a permanent, globally-visible supplier whose domain
     * is literally {@code -}.
     */
    private static final Set<String> PLACEHOLDERS = Set.of(
            "n/a", "na", "n.a.", "-", "--", "—", "–", "none", "null", "nil",
            "tbd", "tba", "unknown", "yok", "bilinmiyor", "belirtilmemiş", "?");

    /** Hosts that are never a company's own domain. */
    private static final Set<String> NON_COMPANY_HOSTS = Set.of(
            "localhost", "example.com", "example.org", "example.net", "test.com");

    /**
     * Normalise a raw website cell, or empty if it does not name a usable domain.
     *
     * <p>Applies: trim, lowercase, strip scheme, strip credentials, strip
     * {@code www.}, drop port/path/query/fragment, then validate shape.
     *
     * <p><strong>Subdomains are deliberately preserved.</strong> Collapsing to a
     * registrable domain would need a public-suffix list, and the naive
     * "last two labels" shortcut is catastrophic in this product's market:
     * {@code firma.com.tr} would reduce to {@code com.tr}, merging every
     * unrelated Turkish company into one supplier row. Preserving subdomains can
     * at worst create a duplicate row for {@code shop.example.com}; collapsing
     * them can merge two real, distinct companies and silently misdirect
     * outreach. A duplicate is the far cheaper mistake, so do not "improve" this
     * into root-domain normalisation without adding a real public-suffix list.
     */
    public static Optional<String> normalize(String raw) {
        if (raw == null) return Optional.empty();

        String value = raw.trim();
        if (value.isEmpty()) return Optional.empty();
        if (PLACEHOLDERS.contains(value.toLowerCase(Locale.ROOT))) return Optional.empty();

        // An email in a website column is common; take the domain half.
        int at = value.lastIndexOf('@');
        if (at >= 0) value = value.substring(at + 1);

        value = value.toLowerCase(Locale.ROOT);

        int scheme = value.indexOf("://");
        if (scheme >= 0) value = value.substring(scheme + 3);

        // Strip any remaining userinfo left by a scheme-less "user@host" form.
        int credentials = value.lastIndexOf('@');
        if (credentials >= 0) value = value.substring(credentials + 1);

        value = cutAt(value, '/');
        value = cutAt(value, '?');
        value = cutAt(value, '#');
        value = cutAt(value, ':');

        while (value.startsWith("www.")) {
            value = value.substring(4);
        }
        while (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }

        if (!isPlausibleDomain(value)) return Optional.empty();
        if (NON_COMPANY_HOSTS.contains(value)) return Optional.empty();

        return Optional.of(value);
    }

    private static String cutAt(String value, char separator) {
        int index = value.indexOf(separator);
        return index >= 0 ? value.substring(0, index) : value;
    }

    /**
     * Shape check only — no DNS lookup, no public-suffix awareness. Requires at
     * least one dot, a TLD of two or more letters, and nothing outside the
     * hostname character set.
     */
    private static boolean isPlausibleDomain(String value) {
        if (value.isEmpty() || value.length() > 500) return false;
        if (!value.contains(".")) return false;
        if (value.startsWith(".") || value.contains("..")) return false;

        String[] labels = value.split("\\.");
        if (labels.length < 2) return false;

        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) return false;
            if (label.startsWith("-") || label.endsWith("-")) return false;
            for (int i = 0; i < label.length(); i++) {
                char c = label.charAt(i);
                boolean allowed = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
                if (!allowed) return false;
            }
        }

        String tld = labels[labels.length - 1];
        if (tld.length() < 2) return false;
        for (int i = 0; i < tld.length(); i++) {
            char c = tld.charAt(i);
            if (c < 'a' || c > 'z') return false;
        }
        return true;
    }

    /** The domain half of an email address, normalised the same way. */
    public static Optional<String> domainOfEmail(String email) {
        if (email == null) return Optional.empty();
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) return Optional.empty();
        return normalize(email.substring(at + 1));
    }
}
