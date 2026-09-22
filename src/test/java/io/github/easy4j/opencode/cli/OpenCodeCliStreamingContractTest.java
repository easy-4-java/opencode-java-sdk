package io.github.easy4j.opencode.cli;

import io.github.easy4j.opencode.OpenCodeCliConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class OpenCodeCliStreamingContractTest {

    @Test
    void streamingExecutionMustDeliverStdoutBeforeProcessExit() throws Exception {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("sh");
        config.setTimeout(5);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        Class<?> handleType = assertDoesNotThrow(
                () -> Class.forName("io.github.easy4j.opencode.cli.OpenCodeCliStreamHandle"));

        Method stream = assertDoesNotThrow(
                () -> OpenCodeCliExecutor.class.getMethod(
                        "stream",
                        OpenCodeCliExecutionContext.class,
                        Consumer.class,
                        Consumer.class,
                        String[].class));

        List<String> stdout = new CopyOnWriteArrayList<>();
        CountDownLatch firstLine = new CountDownLatch(1);
        Consumer<String> stdoutConsumer = line -> {
            stdout.add(line);
            firstLine.countDown();
        };

        Object handle = stream.invoke(
                executor,
                new OpenCodeCliExecutionContext(),
                stdoutConsumer,
                (Consumer<String>) line -> { },
                new String[]{"-c", "printf 'first\\n'; sleep 1; printf 'second\\n'"});

        Method completionMethod = handleType.getMethod("getCompletion");
        @SuppressWarnings("unchecked")
        CompletableFuture<OpenCodeCliResult> completion =
                (CompletableFuture<OpenCodeCliResult>) completionMethod.invoke(handle);

        assertTrue(firstLine.await(700, TimeUnit.MILLISECONDS),
                "first stdout line must be observable while the process is still running");
        assertFalse(completion.isDone(),
                "process should still be running after first line");

        OpenCodeCliResult result = completion.get(4, TimeUnit.SECONDS);
        assertTrue(result.isSuccess());
        assertEquals(Arrays.asList("first", "second"), stdout);
    }

    @Test
    void streamingExecutionMustBeCancellable() throws Exception {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setExecutable("sh");
        config.setTimeout(30);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config);

        Class<?> handleType = assertDoesNotThrow(
                () -> Class.forName("io.github.easy4j.opencode.cli.OpenCodeCliStreamHandle"));
        Method stream = OpenCodeCliExecutor.class.getMethod(
                "stream",
                OpenCodeCliExecutionContext.class,
                Consumer.class,
                Consumer.class,
                String[].class);

        Object handle = stream.invoke(
                executor,
                new OpenCodeCliExecutionContext(),
                (Consumer<String>) line -> { },
                (Consumer<String>) line -> { },
                new String[]{"-c", "sleep 20"});

        Method cancel = handleType.getMethod("cancel");
        Method completionMethod = handleType.getMethod("getCompletion");

        assertEquals(Boolean.TRUE, cancel.invoke(handle));

        @SuppressWarnings("unchecked")
        CompletableFuture<OpenCodeCliResult> completion =
                (CompletableFuture<OpenCodeCliResult>) completionMethod.invoke(handle);
        OpenCodeCliResult result = completion.get(4, TimeUnit.SECONDS);
        assertFalse(result.isSuccess());
    }
}
