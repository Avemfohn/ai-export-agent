package com.aiexportagent.scraping.upload;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * The fields an uploaded exhibitor list can supply, each with the header names
 * real files use for it.
 *
 * <p>Exhibitor lists are exported by hundreds of different fair organisers and
 * agree on nothing. Matching a fixed set of headers would mean the customer
 * hand-reformats every file before uploading — which is the work this feature
 * exists to remove. Aliases cover English and Turkish because those are the two
 * languages the product ships in.
 */
public enum UploadColumn {

    COMPANY_NAME(true, "company", "company name", "companyname", "exhibitor",
            "exhibitor name", "name", "firm", "firma", "firma adi", "sirket",
            "sirket adi", "unvan", "katilimci"),

    WEBSITE(true, "website", "web", "web site", "website url", "url", "site",
            "domain", "homepage", "internet", "web adresi", "web sitesi"),

    COUNTRY(false, "country", "ulke", "nation"),

    CITY(false, "city", "sehir", "il", "town"),

    SECTOR(false, "sector", "industry", "sektor", "category", "kategori",
            "product group", "urun grubu", "faaliyet"),

    DESCRIPTION(false, "description", "about", "profile", "notes", "aciklama",
            "faaliyet konusu", "products", "urunler"),

    CONTACT_NAME(false, "contact", "contact name", "contact person", "yetkili",
            "yetkili kisi", "ilgili kisi", "ad soyad", "full name"),

    CONTACT_TITLE(false, "title", "job title", "position", "unvan2", "gorev",
            "pozisyon"),

    CONTACT_EMAIL(false, "email", "e-mail", "e mail", "eposta", "e-posta",
            "mail", "email address", "eposta adresi"),

    CONTACT_PHONE(false, "phone", "telephone", "tel", "telefon", "gsm",
            "mobile", "phone number");

    /** Without these two a row cannot become a supplier at all. */
    private final boolean required;
    private final List<String> aliases;

    UploadColumn(boolean required, String... aliases) {
        this.required = required;
        this.aliases = List.of(aliases);
    }

    public boolean isRequired() {
        return required;
    }

    /**
     * Whether a spreadsheet header refers to this column.
     *
     * <p>Compared on a folded form: accents and Turkish diacritics stripped,
     * punctuation removed, whitespace collapsed, lowercased. That way
     * {@code "Web Sitesi"}, {@code "web_sitesi"} and {@code "WEB SİTESİ"} all
     * match the same alias without needing an entry each.
     */
    public boolean matches(String header) {
        String folded = fold(header);
        if (folded.isEmpty()) return false;
        return aliases.contains(folded);
    }

    /**
     * Normalise a header for comparison.
     *
     * <p>Turkish dotted/dotless I is handled explicitly before lowercasing:
     * {@code "İ".toLowerCase(ROOT)} yields {@code "i̇"} (i plus a combining dot)
     * rather than {@code "i"}, which would then fail to match {@code "ilgili"}.
     */
    static String fold(String header) {
        if (header == null) return "";
        String value = header
                .replace('İ', 'I')   // İ -> I
                .replace('ı', 'i')   // ı -> i
                .replace('Ş', 'S')   // Ş -> S
                .replace('ş', 's')   // ş -> s
                .replace('Ğ', 'G')   // Ğ -> G
                .replace('ğ', 'g');  // ğ -> g

        value = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        return value.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }
}
