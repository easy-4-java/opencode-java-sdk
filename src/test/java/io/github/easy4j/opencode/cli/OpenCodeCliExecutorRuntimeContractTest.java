package io.github.easy4j.opencode.cli;

import io.github.easy4j.opencode.OpenCodeCliConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OpenCodeCliExecutorRuntimeContractTest {

    @Test
    void maxConcurrentExecutionsMustActuallyLimitChildProcesses() throws Exception {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("sh");
        config.setTimeout(10);
        config.setMaxConcurrentExecutions(1);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        long started = System.nanoTime();
        CompletableFuture<OpenCodeCliResult> first = CompletableFuture.supplyAsync(
                () -> executor.execute("-c", "sleep 1.5; printf first"));
        CompletableFuture<OpenCodeCliResult> second = CompletableFuture.supplyAsync(
                () -> executor.execute("-c", "sleep 1.5; printf second"));

        assertTrue(first.get(8, TimeUnit.SECONDS).isSuccess());
        assertTrue(second.get(8, TimeUnit.SECONDS).isSuccess());

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertTrue(elapsedMs >= 2500,
                "with maxConcurrentExecutions=1 the two 1.5s processes must run serially, elapsed=" + elapsedMs);
    }

    @Test
    void perExecutionEnvironmentMustNotLeakToNextExecution() throws Exception {
        Class<?> contextType = assertDoesNotThrow(
                () -> Class.forName("io.github.easy4j.opencode.cli.OpenCodeCliExecutionContext"));
        Object context = assertDoesNotThrow(() -> contextType.getConstructor().newInstance());

        Method environment = assertDoesNotThrow(
                () -> contextType.getMethod("environment", String.class, String.class));
        environment.invoke(context, "OPENCODE_SDK_TEST_ENV", "scoped-value");

        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("sh");
        config.setTimeout(5);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        Method execute = assertDoesNotThrow(
                () -> OpenCodeCliExecutor.class.getMethod("execute", contextType, String[].class));

        OpenCodeCliResult scoped = (OpenCodeCliResult) execute.invoke(
                executor, context, new String[]{"-c", "printf %s \"$OPENCODE_SDK_TEST_ENV\""});
        assertEquals("scoped-value", scoped.getStdout());

        OpenCodeCliResult next = executor.execute(
                "-c", "printf %s \"\${OPENCODE_SDK_TEST_ENV-unset}\"");
        assertEquals("unset", next.getStdout(),
                "per-execution environment overrides must not mutate later executions");
    }

    @Test
    void stdoutCaptureMustBeBoundedAndMarkedTruncated() throws Exception {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("sh");
        config.setTimeout(5);

        Method setter = assertDoesNotThrow(
                () -> OpenCodeCliConfig.class.getMethod("setMaxStdoutBytes", int.class));
        setter.invoke(config, 128);

        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);
        OpenCodeCliResult result = executor.execute(
                "-c", "head -c 1024 /dev/zero | tr '\\000' x");

        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().length() <= 128,
                "retained stdout must respect maxStdoutBytes");

        Method truncated = assertDoesNotThrow(
                () -> OpenCodeCliResult.class.getMethod("isStdoutTruncated"));
        assertEquals(Boolean.TRUE, truncated.invoke(result));
    }
}
