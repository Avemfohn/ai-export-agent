package com.aiexportagent.scraping.upload;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Decodes uploaded CSV bytes into text, handling the encodings that real
 * exhibitor lists actually arrive in.
 *
 * <p><strong>Why this exists:</strong> Excel on a Turkish system exports CSV in
 * the system ANSI codepage — <strong>Windows-1254</strong> — not UTF-8.
 * Decoding those bytes as UTF-8 corrupts every {@code Ş Ğ İ Ç Ö Ü ı}, and since
 * the result is written into the shared pool the damage is permanent and
 * visible to every tenant. A mangled company name also means a mangled outreach
 * email addressed to a real buyer.
 *
 * <p>Not needed for {@code .xlsx}: OOXML is XML in a zip and is always UTF-8.
 */
public final class CharsetDetector {

    private CharsetDetector() {
    }

    /**
     * Windows-1254 (Turkish). Deliberately not ISO-8859-9, which is close but
     * differs exactly in the range Excel emits for smart quotes and dashes.
     */
    private static final Charset WINDOWS_1254 = Charset.forName("windows-1254");

    /**
     * Decode CSV bytes: honour a BOM if present, otherwise try UTF-8 strictly
     * and fall back to Windows-1254.
     *
     * <p>The fallback is safe in the direction that matters. Turkish text in
     * Windows-1254 contains byte sequences that are not valid UTF-8, so strict
     * UTF-8 decoding <em>fails</em> rather than silently producing mojibake —
     * which is what makes detection-by-attempt reliable here. The reverse
     * (valid UTF-8 misread as Windows-1254) cannot happen because UTF-8 is
     * tried first.
     */
    public static String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";

        if (startsWith(bytes, (byte) 0xEF, (byte) 0xBB, (byte) 0xBF)) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        if (startsWith(bytes, (byte) 0xFF, (byte) 0xFE)) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        if (startsWith(bytes, (byte) 0xFE, (byte) 0xFF)) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }

        return strictDecode(bytes, StandardCharsets.UTF_8)
                .orElseGet(() -> new String(bytes, WINDOWS_1254));
    }

    private static java.util.Optional<String> strictDecode(byte[] bytes, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return java.util.Optional.of(decoder.decode(ByteBuffer.wrap(bytes)).toString());
        } catch (CharacterCodingException e) {
            return java.util.Optional.empty();
        }
    }

    private static boolean startsWith(byte[] bytes, byte... prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) return false;
        }
        return true;
    }
}
