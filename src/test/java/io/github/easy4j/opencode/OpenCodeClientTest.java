package io.github.easy4j.opencode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.api.OpenCodeHttpClient;
import io.github.easy4j.opencode.api.OpenCodeSseClient;
import io.github.easy4j.opencode.cli.OpenCodeCli;
import io.github.easy4j.opencode.cli.OpenCodeCliExecutor;
import io.github.easy4j.opencode.cli.OpenCodeCliResult;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeClient}.
 */
class OpenCodeClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldCreateClientWithHttpConfigOnly() {
        OpenCodeHttpClientConfig httpConfig = new OpenCodeHttpClientConfig();
        httpConfig.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeHttpClient httpClient = new OpenCodeHttpClient(httpConfig, new ObjectMapper(), null);
        OpenCodeClient client = new OpenCodeClient(config, httpClient, null, null);

        assertTrue(client.isHttpEnabled());
        assertFalse(client.isCliEnabled());
        client.close();
    }

    @Test
    void shouldCreateClientWithCliConfigOnly() {
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        config.getHttp().setEnabled(false);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config.getCli());
        OpenCodeCli cli = new OpenCodeCli(executor);
        OpenCodeClient client = new OpenCodeClient(config, null, null, cli);

        assertFalse(client.isHttpEnabled());
        assertTrue(client.isCliEnabled());
        client.close();
    }

    @Test
    void shouldCreateClientWithBothSubsystems() {
        OpenCodeHttpClientConfig httpConfig = new OpenCodeHttpClientConfig();
        httpConfig.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeHttpClient httpClient = new OpenCodeHttpClient(httpConfig, new ObjectMapper(), null);
        OpenCodeCliExecutor executor = new OpenCodeCliExecutor(config.getCli());
        OpenCodeCli cli = new OpenCodeCli(executor);
        OpenCodeClient client = new OpenCodeClient(config, httpClient, null, cli);

        assertTrue(client.isHttpEnabled());
        assertTrue(client.isCliEnabled());
        assertNotNull(client.getConfig());
        assertNotNull(client.cli());
        client.close();
    }

    @Test
    void shouldExposeConfig() {
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeClient client = new OpenCodeClient(config, null, null, null);

        assertNotNull(client.getConfig());
        assertEquals(config, client.getConfig());
        client.close();
    }

    @Test
    void shouldDisableHttpWhenConfigDisabled() {
        OpenCodeHttpClientConfig httpConfig = new OpenCodeHttpClientConfig();
        httpConfig.setEnabled(false);
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        config.getHttp().setEnabled(false);

        // Use the constructor that takes separate configs
        OpenCodeClient client = new OpenCodeClient(
                config,
                (OpenCodeHttpClient) null,
                null,
                null
        );

        assertFalse(client.isHttpEnabled());
        client.close();
    }

    @Test
    void shouldDisableCliWhenConfigDisabled() {
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        config.getCli().setEnabled(false);

        OpenCodeClient client = new OpenCodeClient(config, null, null, null);
        assertFalse(client.isCliEnabled());
        client.close();
    }

    @Test
    void shouldDelegateHealthToHttpClient() {
        OpenCodeHttpClientConfig httpConfig = new OpenCodeHttpClientConfig();
        httpConfig.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"healthy\":true,\"version\":\"1.0.0\"}"));

        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeHttpClient httpClient = new OpenCodeHttpClient(httpConfig, new ObjectMapper(), null);
        OpenCodeClient client = new OpenCodeClient(config, httpClient, null, null);

        var health = client.health();
        assertNotNull(health);
        assertTrue(health.getHealthy());
        client.close();
    }

    @Test
    void shouldDelegateListSessions() {
        OpenCodeHttpClientConfig httpConfig = new OpenCodeHttpClientConfig();
        httpConfig.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"id\":\"sess-1\",\"title\":\"test\"}]"));

        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeHttpClient httpClient = new OpenCodeHttpClient(httpConfig, new ObjectMapper(), null);
        OpenCodeClient client = new OpenCodeClient(config, httpClient, null, null);

        var sessions = client.listSessions();
        assertNotNull(sessions);
        assertEquals(1, sessions.size());
        client.close();
    }

    @Test
    void shouldDelegateListAgents() {
        OpenCodeHttpClientConfig httpConfig = new OpenCodeHttpClientConfig();
        httpConfig.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"name\":\"coder\"}]"));

        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeHttpClient httpClient = new OpenCodeHttpClient(httpConfig, new ObjectMapper(), null);
        OpenCodeClient client = new OpenCodeClient(config, httpClient, null, null);

        var agents = client.listAgents();
        assertNotNull(agents);
        assertEquals(1, agents.size());
        client.close();
    }

    @Test
    void shouldDelegateGetConfig() {
        OpenCodeHttpClientConfig httpConfig = new OpenCodeHttpClientConfig();
        httpConfig.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"theme\":\"dark\"}"));

        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeHttpClient httpClient = new OpenCodeHttpClient(httpConfig, new ObjectMapper(), null);
        OpenCodeClient client = new OpenCodeClient(config, httpClient, null, null);

        var codeConfig = client.getOpenCodeConfig();
        assertNotNull(codeConfig);
        assertEquals("dark", codeConfig.getTheme());
        client.close();
    }

    @Test
    void shouldDelegateAbort() {
        OpenCodeHttpClientConfig httpConfig = new OpenCodeHttpClientConfig();
        httpConfig.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeHttpClient httpClient = new OpenCodeHttpClient(httpConfig, new ObjectMapper(), null);
        OpenCodeClient client = new OpenCodeClient(config, httpClient, null, null);

        assertTrue(client.abort("sess-1"));
        client.close();
    }

    @Test
    void shouldCloseCleanly() {
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        OpenCodeClient client = new OpenCodeClient(config, null, null, null);
        assertDoesNotThrow(client::close);
    }
}
