package io.github.easy4j.opencode.api;

import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.api.event.EventHandler;
import io.github.easy4j.opencode.api.model.Event;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.sse.EventSource;
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
        config.setServerUrl(server.url("/").toString().replaceAll("/$", ""));
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

        AtomicReference<Event> received = new AtomicReference<>();
        EventSource source = sseClient.subscribe(event -> received.set(event));

        Thread.sleep(500);
        assertNotNull(received.get());
        assertEquals("test", received.get().getType());
        source.cancel();
    }

    @Test
    void shouldSubscribeWithContext() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));

        OpenCodeRequestContext context = OpenCodeRequestContext.ofDirectory("/data/project");
        AtomicReference<Event> received = new AtomicReference<>();
        EventSource source = sseClient.subscribe(event -> received.set(event), context);

        Thread.sleep(500);
        assertNotNull(received.get());

        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/data/project", request.getHeader("X-OpenCode-Directory"));
        source.cancel();
    }

    @Test
    void shouldSubscribeQueueAndReceiveEvents() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));

        BlockingQueue<Event> queue = sseClient.subscribeQueue();
        Event event = queue.poll(3, TimeUnit.SECONDS);
        assertNotNull(event);
        assertEquals("test", event.getType());
    }

    @Test
    void shouldSubscribeQueueSubscription() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));

        OpenCodeSseClient.QueueSubscription sub = sseClient.subscribeQueueSubscription(null);
        assertNotNull(sub);
        assertNotNull(sub.getQueue());

        Event event = sub.getQueue().poll(3, TimeUnit.SECONDS);
        assertNotNull(event);
        sub.close();
    }

    @Test
    void shouldFilterBySessionId() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{\"sessionID\":\"sess-1\"}}\n\n" +
                         "data: {\"type\":\"test\",\"properties\":{\"sessionID\":\"sess-2\"}}\n\n"));

        AtomicReference<Event> received = new AtomicReference<>();
        EventSource source = sseClient.subscribeSession("sess-1", event -> received.set(event));

        Thread.sleep(500);
        assertNotNull(received.get());
        assertEquals("sess-1", received.get().getProperties().get("sessionID"));
        source.cancel();
    }

    @Test
    void shouldFilterByEventTypes() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"wanted\",\"properties\":{}}\n\n" +
                         "data: {\"type\":\"unwanted\",\"properties\":{}}\n\n"));

        AtomicReference<Event> received = new AtomicReference<>();
        EventSource source = sseClient.subscribeEventTypes(
                Set.of("wanted"), event -> received.set(event));

        Thread.sleep(500);
        assertNotNull(received.get());
        assertEquals("wanted", received.get().getType());
        source.cancel();
    }

    @Test
    void shouldSubscribeHandler() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"text.delta\",\"properties\":{\"sessionID\":\"sess-1\"}}\n\n"));

        AtomicReference<Event> received = new AtomicReference<>();
        EventHandler handler = new EventHandler() {
            @Override
            public void onEvent(Event event) {
                received.set(event);
            }
        };

        EventSource source = sseClient.subscribeHandler("sess-1", handler);
        Thread.sleep(500);
        assertNotNull(received.get());
        source.cancel();
    }

    @Test
    void shouldRejectNullHandler() {
        assertThrows(IllegalArgumentException.class,
                () -> sseClient.subscribeHandler("sess-1", null));
    }

    @Test
    void shouldStopAllEventSources() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"test\",\"properties\":{}}\n\n"));

        sseClient.subscribe(event -> {});
        sseClient.subscribe(event -> {});

        // Should not throw
        sseClient.stop();
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
        AtomicReference<Event> received = new AtomicReference<>();
        EventHandler handler = new EventHandler() {
            @Override
            public void onEvent(Event event) {
                received.set(event);
            }
        };

        EventSource source = sseClient.subscribeHandler(null, handler, context);
        Thread.sleep(500);
        assertNotNull(received.get());

        var request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/data/proj", request.getHeader("X-OpenCode-Directory"));
        source.cancel();
    }
}
