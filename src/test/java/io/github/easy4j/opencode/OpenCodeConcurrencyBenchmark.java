package io.github.easy4j.opencode;

import io.github.easy4j.opencode.api.sse.SseSubscription;
import io.github.easy4j.opencode.api.model.ChatMessage;
import io.github.easy4j.opencode.api.model.ChatRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** OpenCode SDK HTTP/SSE 串行并发基准入口，由仓库内脚本独立启动并采样。 */
public final class OpenCodeConcurrencyBenchmark {

    private OpenCodeConcurrencyBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: <http|sse> <concurrency> <metrics-file>");
        }
        String workload = args[0];
        int concurrency = Integer.parseInt(args[1]);
        File metricsFile = new File(args[2]);
        if (!("http".equals(workload) || "sse".equals(workload)) || concurrency <= 0) {
            throw new IllegalArgumentException("Invalid benchmark arguments");
        }

        AtomicInteger errors = new AtomicInteger();
        long startedAt = System.nanoTime();
        try (MockWebServer server = new MockWebServer()) {
            enqueueResponses(server, workload, concurrency);
            OpenCodeHttpClientConfig config = config(server, concurrency);
            try (OpenCodeClient client = new OpenCodeClient(config)) {
                if ("http".equals(workload)) {
                    runHttp(client, concurrency, errors);
                } else {
                    runSse(client, concurrency, errors);
                }
            }
            if (server.getRequestCount() != concurrency) {
                errors.addAndGet(Math.abs(concurrency - server.getRequestCount()));
            }
        } catch (Throwable error) {
            errors.incrementAndGet();
            error.printStackTrace(System.err);
        }
        long durationNanos = Math.max(1L, System.nanoTime() - startedAt);
        writeMetrics(metricsFile, concurrency, errors.get(), durationNanos);
        if (errors.get() != 0) {
            System.exit(2);
        }
    }

    private static void runHttp(OpenCodeClient client, int concurrency,
                                AtomicInteger errors) throws Exception {
        List<CompletableFuture<?>> futures = new ArrayList<>(concurrency);
        ChatRequest request = new ChatRequest();
        request.setMessages(Collections.singletonList(new ChatMessage("user", "ping")));
        for (int index = 0; index < concurrency; index++) {
            futures.add(client.chat().chatCompletionAsync("session-benchmark", request)
                    .handle((response, error) -> {
                        if (error != null || response == null) {
                            errors.incrementAndGet();
                        }
                        return null;
                    }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
                .get(90, TimeUnit.SECONDS);
    }

    private static void runSse(OpenCodeClient client, int concurrency,
                               AtomicInteger errors) throws Exception {
        CountDownLatch events = new CountDownLatch(concurrency);
        List<SseSubscription> subscriptions = new ArrayList<>(concurrency);
        for (int index = 0; index < concurrency; index++) {
            try {
                subscriptions.add(client.sse().subscribeEvents(event -> events.countDown()));
            } catch (Throwable error) {
                errors.incrementAndGet();
                events.countDown();
            }
        }
        if (!events.await(90, TimeUnit.SECONDS)) {
            errors.addAndGet((int) events.getCount());
        }
        for (SseSubscription subscription : subscriptions) {
            subscription.close();
        }
    }

    private static void enqueueResponses(MockWebServer server, String workload, int concurrency) {
        for (int index = 0; index < concurrency; index++) {
            MockResponse response;
            if ("http".equals(workload)) {
                response = new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "application/json").setBody("{}");
            } else {
                response = new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "text/event-stream")
                        .setBody("data: {\"type\":\"server.connected\",\"properties\":{}}\n\n");
            }
            server.enqueue(response.setBodyDelay(50, TimeUnit.MILLISECONDS));
        }
    }

    private static OpenCodeHttpClientConfig config(MockWebServer server, int concurrency) {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setBaseUrl(server.url("").toString().replaceAll("/+$", ""));
        config.setStartupCheckEnabled(false);
        int dispatcherLimit = Math.min(256, concurrency);
        config.setMaxRequests(dispatcherLimit);
        config.setMaxRequestsPerHost(dispatcherLimit);
        config.setStreamEventQueueCapacity(Math.max(1024, concurrency));
        return config;
    }

    private static void writeMetrics(File file, int operations, int errors,
                                     long durationNanos) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Cannot create metrics directory: " + parent);
        }
        double throughput = operations * 1_000_000_000.0d / durationNanos;
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("operations=" + operations + "\n");
            writer.write("errors=" + errors + "\n");
            writer.write("duration_seconds=" + (durationNanos / 1_000_000_000.0d) + "\n");
            writer.write("throughput_per_sec=" + throughput + "\n");
        }
    }
}
