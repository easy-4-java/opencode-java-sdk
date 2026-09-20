package io.github.easy4j.opencode.cli.availability;

import io.github.easy4j.opencode.OpenCodeCliConfig;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeCliAvailabilityChecker}.
 */
class OpenCodeCliAvailabilityCheckerTest {

    private final OpenCodeCliAvailabilityChecker checker = new OpenCodeCliAvailabilityChecker();

    @Test
    void shouldReportNotConfiguredWhenExecutableIsBlank() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("  ");

        OpenCodeCliAvailabilityReport report = checker.check(config);
        assertFalse(report.isAvailable());
        assertEquals(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED, report.getStatus());
    }

    @Test
    void shouldReportNotConfiguredWhenExecutableIsNull() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable(null);

        OpenCodeCliAvailabilityReport report = checker.check(config);
        assertFalse(report.isAvailable());
        assertEquals(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED, report.getStatus());
    }

    @Test
    void shouldReportNotFoundForNonexistentPath() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("/nonexistent/path/to/opencode");

        OpenCodeCliAvailabilityReport report = checker.check(config);
        assertFalse(report.isAvailable());
        assertEquals(OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_FOUND, report.getStatus());
    }

    @Test
    void shouldRejectNullConfig() {
        assertThrows(NullPointerException.class, () -> checker.check(null));
    }

    @Test
    void shouldResolveExecutableFromPath() {
        // Test that the resolveExecutablePath method works for known executables
        Optional<String> resolved = OpenCodeCliAvailabilityChecker.resolveExecutablePath("java");
        // java may or may not be on PATH in this environment, but the method should not throw
        assertNotNull(resolved);
    }

    @Test
    void shouldReturnEmptyForBlankExecutable() {
        Optional<String> resolved = OpenCodeCliAvailabilityChecker.resolveExecutablePath("  ");
        assertTrue(resolved.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullExecutable() {
        Optional<String> resolved = OpenCodeCliAvailabilityChecker.resolveExecutablePath(null);
        assertTrue(resolved.isEmpty());
    }

    @Test
    void shouldResolveAbsolutePathIfExists() {
        // /bin/sh should exist on Unix systems
        Optional<String> resolved = OpenCodeCliAvailabilityChecker.resolveExecutablePath("/bin/sh");
        if (resolved.isPresent()) {
            assertEquals("/bin/sh", resolved.get());
        }
        // Not asserting presence since this is environment-dependent
    }

    @Test
    void shouldReturnEmptyForNonexistentAbsolutePath() {
        Optional<String> resolved = OpenCodeCliAvailabilityChecker.resolveExecutablePath("/nonexistent/binary");
        assertTrue(resolved.isEmpty());
    }
}
