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
    void shouldDecodeUtf8OutputFromChildProcess() {
        // 子进程经 POSIX printf 八进制转义输出"你好"的 UTF-8 字节
        //（\344\275\240\345\245\275）。源码必须写双反斜杠：Java 字面量里
        // 的 \344 会被编译器当八进制转义吃掉。
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("sh");
        config.setTimeout(5);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        OpenCodeCliResult result = executor.execute("-c", "printf '\\344\\275\\240\\345\\245\\275'");

        assertTrue(result.isSuccess());
        assertEquals("你好", result.getStdout(), "UTF-8 输出必须按 UTF-8 解码，而非平台默认字符集");
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

        OpenCodeCliResult result = executor.execute("-c", "echo error >&2");
        // sh -c may succeed (exit 0) with stderr captured
        assertNotNull(result.getStdout());
        assertNotNull(result.getStderr());
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
