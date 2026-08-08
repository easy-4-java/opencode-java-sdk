package io.github.easy4j.opencode.exception;

import io.github.easy4j.opencode.cli.availability.OpenCodeCliAvailabilityReport;
import io.github.easy4j.opencode.cli.availability.OpenCodeCliAvailabilityStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for exception classes in {@code io.github.easy4j.opencode.exception}.
 */
class ExceptionClassesTest {

    @Test
    void shouldCreateOpenCodeExceptionWithMessage() {
        OpenCodeException ex = new OpenCodeException("something failed");
        assertEquals("something failed", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void shouldCreateOpenCodeExceptionWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        OpenCodeException ex = new OpenCodeException("wrapped", cause);
        assertEquals("wrapped", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void shouldCreateHttpExceptionWithStatusCodeAndBody() {
        OpenCodeHttpException ex = new OpenCodeHttpException(404, "{\"error\":\"not found\"}");
        assertEquals(404, ex.getStatusCode());
        assertEquals("{\"error\":\"not found\"}", ex.getResponseBody());
        assertTrue(ex.getMessage().contains("404"));
    }

    @Test
    void shouldCreateHttpExceptionWithMessageAndCause() {
        IOException cause = new IOException("connection refused");
        OpenCodeHttpException ex = new OpenCodeHttpException("request failed", cause);
        assertEquals("request failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(-1, ex.getStatusCode());
        assertNull(ex.getResponseBody());
    }

    @Test
    void shouldExtendOpenCodeException() {
        OpenCodeHttpException ex = new OpenCodeHttpException(500, "error");
        assertInstanceOf(OpenCodeException.class, ex);
    }

    @Test
    void shouldCreateCliStartupException() {
        OpenCodeCliAvailabilityReport report = OpenCodeCliAvailabilityReport.builder()
                .status(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_FOUND)
                .available(false)
                .configuredExecutable("opencode")
                .message("not found")
                .build();
        OpenCodeCliStartupException ex = new OpenCodeCliStartupException("CLI not found", report);
        assertEquals("CLI not found", ex.getMessage());
        assertEquals(report, ex.getAvailabilityReport());
        assertFalse(ex.getAvailabilityReport().isAvailable());
    }

    @Test
    void shouldExtendRuntimeException() {
        OpenCodeException ex = new OpenCodeException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }

    // Placeholder for IOException used in the test
    private static class IOException extends Exception {
        IOException(String message) { super(message); }
    }
}
