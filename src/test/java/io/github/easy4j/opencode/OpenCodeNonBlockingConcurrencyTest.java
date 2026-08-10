package io.github.easy4j.opencode;

import io.github.easy4j.opencode.api.OpenCodeChatClient;
import io.github.easy4j.opencode.api.OpenCodeSseClient;
import io.github.easy4j.opencode.api.model.ChatMessage;
import io.github.easy4j.opencode.api.model.ChatRequest;
import io.github.easy4j.opencode.api.sse.SseSubscription;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OpenCode 基于 OkHttp enqueue/EventSource 的 500 并发回归测试。 */
class OpenCodeNonBlockingConcurrencyTest {

    private static final int CONCURRENCY = 500;

    @Test
    void shouldCompleteFiveHundredAsyncChatRequestsWithBoundedDispatcher() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            for (int index = 0; index < CONCURRENCY; index++) {
                server.enqueue(json("{}"));
            }
            OpenCodeHttpClientConfig config = config(server);
            long baseline = countDispatcherThreads();
            ChatRequest request = new ChatRequest();
            request.setMessages(Collections.singletonList(new ChatMessage("user", "ping")));

            try (OpenCodeChatClient client = new OpenCodeChatClient(config)) {
                List<CompletableFuture<?>> futures = new ArrayList<>(CONCURRENCY);
                for (int index = 0; index < CONCURRENCY; index++) {
                    futures.add(client.chatCompletionAsync("session-1", request));
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
                        .get(30, TimeUnit.SECONDS);

                assertEquals(CONCURRENCY, server.getRequestCount());
                assertTrue(countDispatcherThreads() - baseline <= config.getMaxRequests());
            }
        }
    }

    @Test
    void shouldDeliverFiveHundredSseSubscriptionsWithoutConsumerThreads() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            for (int index = 0; index < CONCURRENCY; index++) {
                server.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "text/event-stream")
                        .setBody("data: {\"type\":\"server.connected\",\"properties\":{}}\n\n"));
            }
            OpenCodeHttpClientConfig config = config(server);
            CountDownLatch events = new CountDownLatch(CONCURRENCY);

            try (OpenCodeSseClient sse = new OpenCodeSseClient(config)) {
                List<SseSubscription> subscriptions = new ArrayList<>(CONCURRENCY);
                for (int index = 0; index < CONCURRENCY; index++) {
                    subscriptions.add(sse.subscribeEvents(event -> events.countDown()));
                }
                assertTrue(events.await(30, TimeUnit.SECONDS));
                assertEquals(CONCURRENCY, server.getRequestCount());
                assertEquals(0L, countThreads("opencode-stream-consumer-"));
                subscriptions.forEach(SseSubscription::cancel);
            }
        }
    }

    private OpenCodeHttpClientConfig config(MockWebServer server) {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setBaseUrl(server.url("").toString().replaceAll("/+$", ""));
        config.setStartupCheckEnabled(false);
        config.setMaxRequests(64);
        config.setMaxRequestsPerHost(64);
        return config;
    }

    private MockResponse json(String body) {
        return new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json").setBody(body);
    }

    private long countDispatcherThreads() {
        return countThreads("opencode-okhttp-dispatcher-");
    }

    private long countThreads(String prefix) {
        return Thread.getAllStackTraces().keySet().stream().map(Thread::getName)
                .filter(name -> name.startsWith(prefix)).count();
    }
}
