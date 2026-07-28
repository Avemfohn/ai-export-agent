package com.aiexportagent.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates {@link ApiException} (and its subtypes, e.g. {@link NotFoundException})
 * into a consistent JSON error body.
 *
 * <p>Deliberately does <strong>not</strong> declare a catch-all
 * {@code @ExceptionHandler(Exception.class)}. On an unrestricted
 * {@code @RestControllerAdvice} that would pre-empt Spring's built-in
 * resolution and turn {@code NoResourceFoundException} (404),
 * {@code MethodArgumentTypeMismatchException} (400, e.g. a malformed UUID
 * path variable), {@code HttpRequestMethodNotSupportedException} (405) and
 * {@code HttpMediaTypeNotSupportedException} (415) into 500s — breaking
 * {@code /swagger-ui/**} along with them. Unhandled exceptions are better
 * left to Spring's default 500, which doesn't echo the exception message.
 *
 * <p>Also deliberately does not extend {@code ResponseEntityExceptionHandler}:
 * it emits RFC-7807 {@code ProblemDetail}, whose message field is
 * {@code detail}, while the frontend's {@code apiFetch} reads
 * {@code message}. Every error the UI shows would silently go blank.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    /** Malformed/unparseable JSON request body — reachable from any POST/PATCH. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
    }

    private static ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
