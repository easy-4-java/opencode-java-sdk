package io.github.easy4j.opencode.cli;

import io.github.easy4j.opencode.OpenCodeCliConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeCliExecutor}.
 */
class OpenCodeCliExecutorTest {

    @Test
    void shouldRejectNullConfig() {
        assertThrows(NullPointerException.class, () -> new OpenCodeCliExecutor(null));
    }

    @Test
    void shouldExecuteSimpleCommand() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("echo");
        config.setTimeout(5);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        OpenCodeCliResult result = executor.execute("hello");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("hello"));
    }

    @Test
    void shouldCaptureStderr() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("sh");
        config.setTimeout(5);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        OpenCodeCliResult result = new OpenCodeCliExecutor(config).execute("-c", "echo error >&2");
        assertTrue(result.isSuccess());
        assertTrue(result.getStderr().contains("error"));
    }

    @Test
    void shouldReportFailureForNonExistentCommand() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("/nonexistent/binary");
        config.setTimeout(5);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        OpenCodeCliResult result = executor.execute("--version");
        assertFalse(result.isSuccess());
        assertEquals(-1, result.getExitCode());
    }

    @Test
    void shouldUseWorkingDirectory() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("pwd");
        config.setTimeout(5);
        config.setWorkingDirectory("/tmp");
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        OpenCodeCliResult result = executor.execute();
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("/tmp"));
    }

    @Test
    void shouldHandleNullWorkingDirectory() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("echo");
        config.setTimeout(5);
        config.setWorkingDirectory(null);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        OpenCodeCliResult result = executor.execute("test");
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldHandleBlankWorkingDirectory() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("echo");
        config.setTimeout(5);
        config.setWorkingDirectory("   ");
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        OpenCodeCliResult result = executor.execute("test");
        assertTrue(result.isSuccess());
    }
}
