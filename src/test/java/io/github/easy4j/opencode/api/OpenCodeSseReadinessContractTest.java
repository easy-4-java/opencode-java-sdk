package io.github.easy4j.opencode.api;

import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.api.sse.SseSubscription;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OpenCodeSseReadinessContractTest {

    @Test
    void subscriptionMustExposeConnectedReadiness() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: {\"type\":\"ready\",\"properties\":{}}\n\n"));

            OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
            config.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));

            try (OpenCodeSseClient client = new OpenCodeSseClient(config)) {
                SseSubscription subscription = client.subscribeEvents(event -> { });

                Method getReady = assertDoesNotThrow(
                        () -> SseSubscription.class.getMethod("getReady"));
                @SuppressWarnings("unchecked")
                CompletableFuture<Void> ready =
                        (CompletableFuture<Void>) getReady.invoke(subscription);

                assertDoesNotThrow(() -> ready.get(2, TimeUnit.SECONDS));
                subscription.cancel();
            }
        }
    }

    @Test
    void connectionFailureMustFailReadinessAndExposeFailure() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

            OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
            config.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));

            try (OpenCodeSseClient client = new OpenCodeSseClient(config)) {
                SseSubscription subscription = client.subscribeEvents(event -> { });

                Method getReady = assertDoesNotThrow(
                        () -> SseSubscription.class.getMethod("getReady"));
                Method getFailure = assertDoesNotThrow(
                        () -> SseSubscription.class.getMethod("getFailure"));

                @SuppressWarnings("unchecked")
                CompletableFuture<Void> ready =
                        (CompletableFuture<Void>) getReady.invoke(subscription);
                @SuppressWarnings("unchecked")
                CompletableFuture<Throwable> failure =
                        (CompletableFuture<Throwable>) getFailure.invoke(subscription);

                assertThrows(ExecutionException.class,
                        () -> ready.get(2, TimeUnit.SECONDS));
                assertNotNull(failure.get(2, TimeUnit.SECONDS));
            }
        }
    }
}
