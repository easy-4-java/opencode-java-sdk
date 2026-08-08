package io.github.easy4j.opencode.cli.availability;

import io.github.easy4j.opencode.cli.OpenCodeCliResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeCliAvailabilityReport}.
 */
class OpenCodeCliAvailabilityReportTest {

    @Test
    void shouldReportAvailable() {
        OpenCodeCliAvailabilityReport report = OpenCodeCliAvailabilityReport.builder()
                .status(OpenCodeCliAvailabilityStatus.AVAILABLE)
                .available(true)
                .configuredExecutable("opencode")
                .resolvedExecutablePath("/usr/local/bin/opencode")
                .message("opencode --version succeeded")
                .probeResult(new OpenCodeCliResult(0, "opencode 1.0.0", ""))
                .build();

        assertTrue(report.isAvailable());
        assertEquals(OpenCodeCliAvailabilityStatus.AVAILABLE, report.getStatus());
        assertEquals("opencode", report.getConfiguredExecutable());
        assertEquals("/usr/local/bin/opencode", report.getResolvedExecutablePath());
        assertEquals("opencode --version succeeded", report.getMessage());
        assertNotNull(report.getProbeResult());
    }

    @Test
    void shouldReportUnavailable() {
        OpenCodeCliAvailabilityReport report = OpenCodeCliAvailabilityReport.builder()
                .status(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_FOUND)
                .available(false)
                .configuredExecutable("opencode")
                .message("executable not found on PATH")
                .build();

        assertFalse(report.isAvailable());
        assertEquals(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_FOUND, report.getStatus());
    }

    @Test
    void shouldGenerateDiagnosticMessageForAvailable() {
        OpenCodeCliAvailabilityReport report = OpenCodeCliAvailabilityReport.builder()
                .status(OpenCodeCliAvailabilityStatus.AVAILABLE)
                .available(true)
                .configuredExecutable("opencode")
                .resolvedExecutablePath("/usr/local/bin/opencode")
                .message("opencode --version succeeded")
                .build();

        String diagnostic = report.toDiagnosticMessage();
        assertNotNull(diagnostic);
        assertTrue(diagnostic.contains("ready"));
        assertTrue(diagnostic.contains("AVAILABLE"));
        assertTrue(diagnostic.contains("opencode"));
    }

    @Test
    void shouldGenerateDiagnosticMessageForUnavailable() {
        OpenCodeCliAvailabilityReport report = OpenCodeCliAvailabilityReport.builder()
                .status(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_FOUND)
                .available(false)
                .configuredExecutable("opencode")
                .message("not found")
                .build();

        String diagnostic = report.toDiagnosticMessage();
        assertNotNull(diagnostic);
        assertTrue(diagnostic.contains("unavailable"));
        assertTrue(diagnostic.contains("EXECUTABLE_NOT_FOUND"));
        assertTrue(diagnostic.contains("not found"));
    }

    @Test
    void shouldHandleNullFieldsInDiagnostic() {
        OpenCodeCliAvailabilityReport report = OpenCodeCliAvailabilityReport.builder()
                .status(OpenCodeCliAvailabilityStatus.FAILED)
                .available(false)
                .build();

        String diagnostic = report.toDiagnosticMessage();
        assertNotNull(diagnostic);
        assertTrue(diagnostic.contains("unavailable"));
    }
}
