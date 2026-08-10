package io.github.easy4j.opencode.api;

import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.HttpCallCancellation;
import io.github.easy4j.opencode.exception.OpenCodeHttpException;
import io.github.easy4j.opencode.api.model.PromptRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OpenCodeHttpClient} 工作区路由测试 + 新增端点路径覆盖。
 *
 * @author [@Loong Wan](https://github.com/loong10k)
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
        config.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
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
        assertTrue(client.promptAsync(sessionId, PromptRequest.ofText("hello"), context).join());

        for (int index = 0; index < 3; index++) {
            RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("/data/opencode/workspaces/t1/p1",
                    request.getHeader("X-OpenCode-Directory"));
        }
    }

    @Test
    void shouldCancelSessionLookupWithoutCreatingAnotherSession() {
        AtomicBoolean cancelled = new AtomicBoolean();
        HttpCallCancellation cancellation = new HttpCallCancellation() {
            @Override
            public AutoCloseable onCancel(Runnable callback) {
                cancelled.set(true);
                callback.run();
                return () -> { };
            }

            @Override
            public boolean isCancelled() {
                return cancelled.get();
            }
        };

        assertThrows(OpenCodeHttpException.class,
                () -> client.chatCompletionWithSession(PromptRequest.ofText("hello"),
                        "stable-key", cancellation));
        assertTrue(cancelled.get());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void shouldRouteEventStreamToDirectory() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: {\"type\":\"server.connected\",\"properties\":{}}\n\n"));
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
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

    // ============================================================
    // 新增端点路径覆盖（opencode v1.17.18 适配）
    // ============================================================

    @Test
    void getConfigHitsConfigEndpoint() throws InterruptedException {
        server.enqueue(json("{}"));
        client.getConfig();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/config", request.getPath());
    }

    @Test
    void updateConfigUsesPatch() throws InterruptedException {
        server.enqueue(json("{}"));
        client.updateConfig(Collections.singletonMap("theme", "dark"));
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("PATCH", request.getMethod());
        assertEquals("/config", request.getPath());
    }

    @Test
    void listProjectsHitsProjectEndpoint() throws InterruptedException {
        server.enqueue(json("[]"));
        client.listProjects();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/project", request.getPath());
    }

    @Test
    void listProvidersHitsProviderEndpoint() throws InterruptedException {
        server.enqueue(json("{\"all\":[],\"connected\":[]}"));
        client.listProviders();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/provider", request.getPath());
    }

    @Test
    void listFilesUsesPathQuery() throws InterruptedException {
        server.enqueue(json("[]"));
        client.listFiles("/data/projects/foo");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertTrue(request.getPath().startsWith("/file?"));
        assertTrue(request.getPath().contains("path="));
    }

    @Test
    void findUsesPatternQuery() throws InterruptedException {
        server.enqueue(json("[]"));
        client.find("OpenCodeClient");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertTrue(request.getPath().startsWith("/find?"));
        assertTrue(request.getPath().contains("pattern=OpenCodeClient"));
    }

    @Test
    void findFilesUsesQueryParameter() throws InterruptedException {
        server.enqueue(json("[]"));
        client.findFiles("OpenCodeHttpClient");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertTrue(request.getPath().startsWith("/find/file?"));
        assertTrue(request.getPath().contains("query=OpenCodeHttpClient"));
    }

    @Test
    void listCommandsHitsCommandEndpoint() throws InterruptedException {
        server.enqueue(json("[]"));
        client.listCommands();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/command", request.getPath());
    }

    @Test
    void listSkillsHitsSkillEndpoint() throws InterruptedException {
        server.enqueue(json("[]"));
        client.listSkills();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/skill", request.getPath());
    }

    @Test
    void listFormattersHitsFormatterEndpoint() throws InterruptedException {
        server.enqueue(json("[]"));
        client.listFormatters();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/formatter", request.getPath());
    }

    @Test
    void listLspsHitsLspEndpoint() throws InterruptedException {
        server.enqueue(json("[]"));
        client.listLsps();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/lsp", request.getPath());
    }

    @Test
    void listMcpServersHitsMcpEndpoint() throws InterruptedException {
        server.enqueue(json("{}"));
        client.listMcpServers();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/mcp", request.getPath());
    }

    @Test
    void getVcsHitsVcsEndpoint() throws InterruptedException {
        server.enqueue(json("{}"));
        client.getVcs();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/vcs", request.getPath());
    }

    @Test
    void getPathHitsPathEndpoint() throws InterruptedException {
        server.enqueue(json("{}"));
        client.getPath();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/path", request.getPath());
    }

    @Test
    void shareSessionUsesShareEndpoint() throws InterruptedException {
        server.enqueue(json("{\"id\":\"sess-1\",\"title\":\"share\"}"));
        client.shareSession("sess-1");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/session/sess-1/share", request.getPath());
    }

    @Test
    void unshareSessionUsesDelete() throws InterruptedException {
        server.enqueue(json("{\"id\":\"sess-1\"}"));
        client.unshareSession("sess-1");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("DELETE", request.getMethod());
        assertEquals("/session/sess-1/share", request.getPath());
    }

    @Test
    void forkSessionUsesForkEndpoint() throws InterruptedException {
        server.enqueue(json("{\"id\":\"sess-fork\"}"));
        client.forkSession("sess-orig", "msg-1");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/session/sess-orig/fork", request.getPath());
    }

    @Test
    void summarizeSessionUsesSummarizeEndpoint() throws InterruptedException {
        server.enqueue(json("{}"));
        client.summarizeSession("sess-1", "anthropic", "claude-sonnet-4-5");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/session/sess-1/summarize", request.getPath());
    }

    @Test
    void revertSessionUsesRevertEndpoint() throws InterruptedException {
        server.enqueue(json("{}"));
        client.revertSession("sess-1", "msg-2", null);
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/session/sess-1/revert", request.getPath());
    }

    @Test
    void getMessageUsesMessageEndpoint() throws InterruptedException {
        server.enqueue(json("{\"info\":{},\"parts\":[]}"));
        client.getMessage("sess-1", "msg-1");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/session/sess-1/message/msg-1", request.getPath());
    }

    @Test
    void runSessionCommandUsesCommandEndpoint() throws InterruptedException {
        server.enqueue(json("{\"info\":{},\"parts\":[]}"));
        client.runSessionCommand("sess-1", "/help", null, null, null);
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/session/sess-1/command", request.getPath());
    }

    @Test
    void replyQuestionUsesQuestionReplyEndpoint() throws InterruptedException {
        server.enqueue(json("{}"));
        client.replyQuestion("q-1", Collections.singletonList("option-a"));
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/question/q-1/reply", request.getPath());
    }

    @Test
    void replyPermissionUsesPermissionReplyEndpoint() throws InterruptedException {
        server.enqueue(json("{}"));
        client.replyPermission("p-1", "approve", true);
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/permission/p-1/reply", request.getPath());
    }

    @Test
    void setAuthUsesPutMethod() throws InterruptedException {
        server.enqueue(json("{}"));
        client.setAuth("anthropic", Collections.singletonMap("apiKey", "sk-xxx"));
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("PUT", request.getMethod());
        assertEquals("/auth/anthropic", request.getPath());
    }

    @Test
    void removeAuthUsesDeleteMethod() throws InterruptedException {
        server.enqueue(json("{}"));
        client.removeAuth("anthropic");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("DELETE", request.getMethod());
        assertEquals("/auth/anthropic", request.getPath());
    }

    @Test
    void disposeInstanceUsesInstanceDispose() throws InterruptedException {
        server.enqueue(json("{}"));
        client.disposeInstance();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/instance/dispose", request.getPath());
    }

    @Test
    void globalDisposeUsesGlobalDispose() throws InterruptedException {
        server.enqueue(json("{}"));
        client.globalDispose();
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/global/dispose", request.getPath());
    }

    @Test
    void globalUpgradeUsesGlobalUpgrade() throws InterruptedException {
        server.enqueue(json("{}"));
        client.globalUpgrade("v1.18.0");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/global/upgrade", request.getPath());
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
