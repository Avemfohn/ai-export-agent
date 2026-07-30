package com.aiexportagent.common.validation;

import com.aiexportagent.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No Spring context — the validator is deliberately static and stateless so
 * these run instantly and without a database.
 */
class SettingsJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String raw) {
        try {
            return MAPPER.readTree(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("bad test fixture: " + raw, e);
        }
    }

    @Nested
    class BuyerCriteria {

        @Test
        void acceptsAnyObjectShape() {
            // The whole point of "opaque JSON": we validate shape, never contents.
            assertThatCode(() -> SettingsJsonValidator.validateBuyerCriteria(json("{}"), "Buyer criteria"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> SettingsJsonValidator.validateBuyerCriteria(
                    json("""
                            {"minAnnualRevenueUsd":500000,"importsFromTurkey":true,"keywords":["towels"]}"""),
                    "Buyer criteria"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> SettingsJsonValidator.validateBuyerCriteria(
                    json("""
                            {"somethingWeNeverAnticipated":{"deeply":{"nested":[1,2,3]}}}"""),
                    "Buyer criteria"))
                    .doesNotThrowAnyException();
        }

        @Test
        void rejectsNonObjects() {
            // An array here would corrupt the scoring prompt envelope.
            assertBadRequest(() -> SettingsJsonValidator.validateBuyerCriteria(json("[]"), "Buyer criteria"),
                    "Buyer criteria must be a JSON object");
            assertBadRequest(() -> SettingsJsonValidator.validateBuyerCriteria(json("\"text\""), "Buyer criteria"),
                    "Buyer criteria must be a JSON object");
            assertBadRequest(() -> SettingsJsonValidator.validateBuyerCriteria(json("42"), "Buyer criteria"),
                    "Buyer criteria must be a JSON object");
            assertBadRequest(() -> SettingsJsonValidator.validateBuyerCriteria(null, "Buyer criteria"),
                    "Buyer criteria must be a JSON object");
        }

        @Test
        void rejectsOversizedPayload() {
            String hugeValue = "x".repeat(9000);
            assertBadRequest(
                    () -> SettingsJsonValidator.validateBuyerCriteria(
                            json("{\"k\":\"" + hugeValue + "\"}"), "Buyer criteria"),
                    "too large");
        }
    }

    @Nested
    class StringArrays {

        @Test
        void acceptsArrayOfNonBlankStrings() {
            assertThatCode(() -> SettingsJsonValidator.validateStringArray(
                    json("""
                            ["home textiles","bedding"]"""), "Target sectors"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> SettingsJsonValidator.validateStringArray(json("[]"), "Target sectors"))
                    .doesNotThrowAnyException();
        }

        @Test
        void rejectsNonArrays() {
            assertBadRequest(() -> SettingsJsonValidator.validateStringArray(json("{}"), "Target sectors"),
                    "Target sectors must be a JSON array");
            assertBadRequest(() -> SettingsJsonValidator.validateStringArray(null, "Target sectors"),
                    "Target sectors must be a JSON array");
        }

        @Test
        void rejectsBlankOrNonTextElements() {
            assertBadRequest(() -> SettingsJsonValidator.validateStringArray(
                            json("""
                                    ["ok","  "]"""), "Target sectors"),
                    "non-empty text values");
            assertBadRequest(() -> SettingsJsonValidator.validateStringArray(json("[1,2]"), "Target sectors"),
                    "non-empty text values");
        }

        @Test
        void rejectsTooManyEntries() {
            String many = "[" + "\"a\",".repeat(50) + "\"a\"]";
            assertBadRequest(() -> SettingsJsonValidator.validateStringArray(json(many), "Target sectors"),
                    "at most 50 entries");
        }
    }

    @Nested
    class EmailDraftTemplate {

        @Test
        void acceptsValidTemplateAndPreservesUnknownKeys() {
            assertThatCode(() -> SettingsJsonValidator.validateEmailDraftTemplate(
                    json("""
                            {"subject":"Hello {{companyName}}","body":"Hi {{contactFirstName}},\\n\\nRegards","notes":"be brief","extra":"kept"}"""),
                    "Email template"))
                    .doesNotThrowAnyException();
        }

        @Test
        void notesAreOptional() {
            assertThatCode(() -> SettingsJsonValidator.validateEmailDraftTemplate(
                    json("""
                            {"subject":"s","body":"b"}"""), "Email template"))
                    .doesNotThrowAnyException();
        }

        @Test
        void rejectsEmptyObject() {
            // This is the exact shape the column default ('{}') produces, and the
            // one that silently sends blank emails.
            assertBadRequest(
                    () -> SettingsJsonValidator.validateEmailDraftTemplate(json("{}"), "Email template"),
                    "Email template subject is required");
        }

        @Test
        void rejectsBlankSubjectOrBody() {
            assertBadRequest(() -> SettingsJsonValidator.validateEmailDraftTemplate(
                            json("""
                                    {"subject":"   ","body":"b"}"""), "Email template"),
                    "Email template subject is required");
            assertBadRequest(() -> SettingsJsonValidator.validateEmailDraftTemplate(
                            json("""
                                    {"subject":"s","body":""}"""), "Email template"),
                    "Email template body is required");
        }

        @Test
        void rejectsNonTextFields() {
            assertBadRequest(() -> SettingsJsonValidator.validateEmailDraftTemplate(
                            json("""
                                    {"subject":123,"body":"b"}"""), "Email template"),
                    "Email template subject must be text");
        }

        @Test
        void rejectsOversizedSubject() {
            assertBadRequest(() -> SettingsJsonValidator.validateEmailDraftTemplate(
                            json("{\"subject\":\"" + "s".repeat(301) + "\",\"body\":\"b\"}"), "Email template"),
                    "300 characters or fewer");
        }
    }

    private static void assertBadRequest(Runnable action, String expectedFragment) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(expectedFragment)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
