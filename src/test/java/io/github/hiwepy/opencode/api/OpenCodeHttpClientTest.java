package io.github.hiwepy.opencode.api;

import io.github.hiwepy.opencode.OpenCodeHttpClientConfig;
import io.github.hiwepy.opencode.api.model.PromptRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OpenCodeHttpClient} 工作区路由测试。
 *
 * @author wandl
 * @since 2.7.x
 */
class OpenCodeHttpClientTest {

    private MockWebServer server;
    private OpenCodeHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setServerUrl(server.url("/").toString().replaceAll("/$", ""));
        client = new OpenCodeHttpClient(config, null, null);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.shutdown();
    }

    @Test
    void shouldRouteSessionLifecycleToDirectory() throws InterruptedException {
        server.enqueue(json("[]"));
        server.enqueue(json("{\"id\":\"session-1\",\"title\":\"stable-key\"}"));
        server.enqueue(json("{}"));

        OpenCodeRequestContext context = OpenCodeRequestContext.ofDirectory("/data/opencode/workspaces/t1/p1");
        String sessionId = client.ensureSession("stable-key", context);
        assertEquals("session-1", sessionId);
        assertTrue(client.promptAsync(sessionId, PromptRequest.ofText("hello"), context));

        for (int index = 0; index < 3; index++) {
            RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("/data/opencode/workspaces/t1/p1",
                    request.getHeader("X-OpenCode-Directory"));
        }
    }

    @Test
    void shouldRouteEventStreamToDirectory() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"server.connected\",\"properties\":{}}\n\n"));
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setServerUrl(server.url("/").toString().replaceAll("/$", ""));
        OpenCodeSseClient sseClient = new OpenCodeSseClient(
                config, client.getObjectMapper(), client.getOkHttpClient());
        try {
            sseClient.subscribe(event -> { },
                    OpenCodeRequestContext.ofDirectory("/data/opencode/workspaces/t1/research-1"));
            RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("/data/opencode/workspaces/t1/research-1",
                    request.getHeader("X-OpenCode-Directory"));
        } finally {
            sseClient.close();
        }
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
