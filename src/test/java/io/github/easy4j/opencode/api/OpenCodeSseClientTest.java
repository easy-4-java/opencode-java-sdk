package io.github.easy4j.opencode.api;

import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.api.event.EventHandler;
import io.github.easy4j.opencode.api.sse.SseEvent;
import io.github.easy4j.opencode.api.sse.SseQueueSubscription;
import io.github.easy4j.opencode.api.sse.SseSubscription;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeSseClient}.
 */
class OpenCodeSseClientTest {

    private MockWebServer server;
    private OpenCodeSseClient sseClient;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        sseClient = new OpenCodeSseClient(config, null, null);
    }

    @AfterEach
    void tearDown() {
        sseClient.close();
        try {
            server.shutdown();
        } catch (IOException ignored) {
        }
    }

    @Test
    void shouldSubscribeAndReceiveEvent() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{\"key\":\"value\"}}\n\n"));

        AtomicReference<SseEvent> received = new AtomicReference<>();
        SseSubscription subscription = sseClient.subscribeEvents(received::set);

        Thread.sleep(500);
        assertNotNull(received.get());
        assertEquals("test", received.get().getType());
        subscription.cancel();
    }

    @Test
    void shouldSubscribeWithContext() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));

        OpenCodeRequestContext context = OpenCodeRequestContext.ofDirectory("/data/project");
        AtomicReference<SseEvent> received = new AtomicReference<>();
        SseSubscription subscription = sseClient.subscribeEvents(received::set, context);

        Thread.sleep(500);
        assertNotNull(received.get());

        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/data/project", request.getHeader("X-OpenCode-Directory"));
        subscription.cancel();
    }

    @Test
    void shouldSubscribeQueueSubscription() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));

        SseQueueSubscription sub = sseClient.subscribeEventsQueue(null);
        assertNotNull(sub);
        assertNotNull(sub.getQueue());

        SseEvent event = sub.getQueue().poll(3, TimeUnit.SECONDS);
        assertNotNull(event);
        sub.close();
    }

    @Test
    void shouldFilterBySessionId() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{\"sessionID\":\"sess-1\"}}\n\n" +
                         "data: {\"type\":\"test\",\"properties\":{\"sessionID\":\"sess-2\"}}\n\n"));

        AtomicReference<SseEvent> received = new AtomicReference<>();
        SseSubscription subscription = sseClient.subscribeSessionEvents("sess-1", received::set);

        Thread.sleep(500);
        assertNotNull(received.get());
        assertEquals("sess-1", received.get().getProperties().get("sessionID"));
        subscription.cancel();
    }

    @Test
    void shouldFilterByEventTypes() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"wanted\",\"properties\":{}}\n\n" +
                         "data: {\"type\":\"unwanted\",\"properties\":{}}\n\n"));

        AtomicReference<SseEvent> received = new AtomicReference<>();
        SseSubscription subscription = sseClient.subscribeEventTypes(
                Set.of("wanted"), received::set);

        Thread.sleep(500);
        assertNotNull(received.get());
        assertEquals("wanted", received.get().getType());
        subscription.cancel();
    }

    @Test
    void shouldSubscribeHandler() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"text.delta\",\"properties\":{\"sessionID\":\"sess-1\"}}\n\n"));

        AtomicReference<SseEvent> received = new AtomicReference<>();
        EventHandler handler = new EventHandler() {
            @Override
            public void onEvent(SseEvent event) {
                received.set(event);
            }
        };

        SseSubscription subscription = sseClient.subscribeSessionEvents("sess-1", handler);
        Thread.sleep(500);
        assertNotNull(received.get());
        subscription.cancel();
    }

    @Test
    void shouldRejectNullHandler() {
        assertThrows(NullPointerException.class,
                () -> sseClient.subscribeSessionEvents("sess-1", (EventHandler) null));
    }

    @Test
    void shouldCloseAllSubscriptions() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));

        sseClient.subscribeEvents(event -> { });
        sseClient.subscribeEvents(event -> { });

        assertEquals(2, sseClient.activeSubscriptionCount());
        sseClient.close();
        assertEquals(0, sseClient.activeSubscriptionCount());
    }

    @Test
    void shouldCloseIdempotently() {
        sseClient.close();
        sseClient.close(); // second close should not throw
    }

    @Test
    void shouldSubscribeHandlerWithContext() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));

        OpenCodeRequestContext context = OpenCodeRequestContext.ofDirectory("/data/proj");
        AtomicReference<SseEvent> received = new AtomicReference<>();
        EventHandler handler = new EventHandler() {
            @Override
            public void onEvent(SseEvent event) {
                received.set(event);
            }
        };

        SseSubscription subscription = sseClient.subscribeSessionEvents(null, handler, context);
        Thread.sleep(500);
        assertNotNull(received.get());

        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/data/proj", request.getHeader("X-OpenCode-Directory"));
        subscription.cancel();
    }
}
