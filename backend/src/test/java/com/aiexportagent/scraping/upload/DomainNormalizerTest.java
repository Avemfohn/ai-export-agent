package com.aiexportagent.scraping.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DomainNormalizer} is the dedup contract for the shared supplier pool —
 * if it is wrong, the UNIQUE constraint on {@code global_suppliers.domain} never
 * fires and the pool fills with duplicates of the same company.
 */
class DomainNormalizerTest {

    @Nested
    @DisplayName("collapses the forms that mean the same company")
    class Dedup {

        @Test
        void schemeWwwPathAndCaseAllCollapseToOneDomain() {
            String expected = "meadowbrookhome.com";
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("meadowbrookhome.com"));
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("www.meadowbrookhome.com"));
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("http://meadowbrookhome.com"));
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("https://www.meadowbrookhome.com"));
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("HTTPS://WWW.MeadowbrookHome.COM"));
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("https://meadowbrookhome.com/en/about"));
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("https://meadowbrookhome.com/x?a=1#top"));
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("meadowbrookhome.com:8080"));
            assertEquals(Optional.of(expected), DomainNormalizer.normalize("  meadowbrookhome.com.  "));
        }

        @Test
        void extractsDomainWhenAnEmailLandsInTheWebsiteColumn() {
            assertEquals(Optional.of("meadowbrookhome.com"),
                    DomainNormalizer.normalize("info@meadowbrookhome.com"));
        }
    }

    @Nested
    @DisplayName("keeps companies that are genuinely different apart")
    class NoOverCollapsing {

        /**
         * The reason subdomains are preserved. Collapsing to "last two labels"
         * would turn every firma.com.tr into com.tr and merge unrelated Turkish
         * companies into a single shared-pool row.
         */
        @Test
        void turkishCommercialDomainsAreNotCollapsedToTheSuffix() {
            assertEquals(Optional.of("firma.com.tr"), DomainNormalizer.normalize("https://www.firma.com.tr"));
            assertEquals(Optional.of("baskafirma.com.tr"), DomainNormalizer.normalize("baskafirma.com.tr"));
        }

        @Test
        void subdomainsArePreserved() {
            assertEquals(Optional.of("shop.meadowbrookhome.com"),
                    DomainNormalizer.normalize("https://shop.meadowbrookhome.com"));
        }
    }

    @Nested
    @DisplayName("rejects cells that do not name a domain")
    class Rejections {

        @Test
        void placeholdersAreNotDomains() {
            for (String placeholder : new String[]{"N/A", "n/a", "-", "--", "none", "NULL", "TBD", "Yok", "?", ""}) {
                assertTrue(DomainNormalizer.normalize(placeholder).isEmpty(),
                        () -> "expected no domain for placeholder: " + placeholder);
            }
        }

        @Test
        void malformedValuesAreRejected() {
            for (String bad : new String[]{"notadomain", "acme", ".com", "acme..com",
                    "-acme.com", "acme.c", "ac me.com", "http://", "acme.123"}) {
                assertTrue(DomainNormalizer.normalize(bad).isEmpty(),
                        () -> "expected rejection for: " + bad);
            }
        }

        /**
         * RFC 2606 reserves these, so they can never be a real company. In an
         * exhibitor list they mean a leftover template row — precisely the
         * sloppy-file case that must not reach the shared pool.
         */
        @Test
        void reservedDocumentationDomainsAreRejected() {
            assertTrue(DomainNormalizer.normalize("example.com").isEmpty());
            assertTrue(DomainNormalizer.normalize("https://www.example.org").isEmpty());
            assertTrue(DomainNormalizer.normalize("localhost").isEmpty());
        }

        @Test
        void nullIsEmptyNotAnException() {
            assertTrue(DomainNormalizer.normalize(null).isEmpty());
        }
    }

    @Nested
    class EmailDomains {

        @Test
        void extractsAndNormalisesTheDomainHalf() {
            assertEquals(Optional.of("meadowbrookhome.com"),
                    DomainNormalizer.domainOfEmail("Sarah@WWW.MeadowbrookHome.com"));
        }

        @Test
        void rejectsNonAddresses() {
            assertTrue(DomainNormalizer.domainOfEmail("not-an-email").isEmpty());
            assertTrue(DomainNormalizer.domainOfEmail("trailing@").isEmpty());
            assertTrue(DomainNormalizer.domainOfEmail(null).isEmpty());
        }
    }
}
