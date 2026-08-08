package io.github.easy4j.opencode.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeCliResult}.
 */
class OpenCodeCliResultTest {

    @Test
    void shouldReportSuccessForZeroExitCode() {
        OpenCodeCliResult result = new OpenCodeCliResult(0, "output", "");
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertEquals("output", result.getStdout());
        assertEquals("", result.getStderr());
    }

    @Test
    void shouldReportFailureForNonZeroExitCode() {
        OpenCodeCliResult result = new OpenCodeCliResult(1, "", "error");
        assertFalse(result.isSuccess());
        assertEquals(1, result.getExitCode());
    }

    @Test
    void shouldReportFailureForNegativeExitCode() {
        OpenCodeCliResult result = new OpenCodeCliResult(-1, "", "spawn failed");
        assertFalse(result.isSuccess());
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        OpenCodeCliResult r1 = new OpenCodeCliResult(0, "out", "err");
        OpenCodeCliResult r2 = new OpenCodeCliResult(0, "out", "err");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        OpenCodeCliResult result = new OpenCodeCliResult(0, "out", "err");
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("exitCode"));
    }
}
