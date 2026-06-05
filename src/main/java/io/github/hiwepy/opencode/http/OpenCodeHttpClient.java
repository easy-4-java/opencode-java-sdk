package io.github.hiwepy.opencode.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.OpenCodeClientConfig;
import io.github.hiwepy.opencode.exception.OpenCodeHttpException;
import io.github.hiwepy.opencode.model.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenCode Server HTTP 客户端，封装 REST API。
 *
 * @see <a href="https://opencode.ai/docs/server/">opencode server docs</a>
 */
public class OpenCodeHttpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeHttpClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OpenCodeClientConfig config;
    private final CloseableHttpClient http;

    public OpenCodeHttpClient(OpenCodeClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.http = buildHttpClient(config);
    }

    // ============================================================
    // Global
    // ============================================================

    public HealthStatus health() {
        return get("/global/health", HealthStatus.class);
    }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) {
        Map<String, Object> body = title != null ? Map.of("title", title) : Map.of();
        return post("/session", body, Session.class);
    }

    public Session getSession(String sessionId) {
        return get("/session/" + sessionId, Session.class);
    }

    public List<Session> listSessions() {
        String json = getRaw("/session");
        return parseList(json, new TypeReference<List<Session>>() {});
    }

    public boolean deleteSession(String sessionId) {
        return delete("/session/" + sessionId);
    }

    // ============================================================
    // Message / Prompt
    // ============================================================

    /**
     * 发送消息并同步等待 AI 响应（POST /session/:id/message）。
     */
    public PromptResult prompt(String sessionId, PromptRequest request) {
        return post("/session/" + sessionId + "/message", request, PromptResult.class);
    }

    /**
     * 异步发送消息，不等待响应（POST /session/:id/prompt_async）。
     */
    public boolean promptAsync(String sessionId, PromptRequest request) {
        return postNoContent("/session/" + sessionId + "/prompt_async", request);
    }

    public List<PromptResult> getMessages(String sessionId) {
        String json = getRaw("/session/" + sessionId + "/message");
        return parseList(json, new TypeReference<List<PromptResult>>() {});
    }

    public boolean abortSession(String sessionId) {
        return postNoContent("/session/" + sessionId + "/abort", null);
    }

    // ============================================================
    // Agent
    // ============================================================

    public List<Agent> listAgents() {
        String json = getRaw("/agent");
        return parseList(json, new TypeReference<List<Agent>>() {});
    }

    // ============================================================
    // Internal HTTP helpers
    // ============================================================

    private <T> T get(String path, Class<T> type) {
        String json = getRaw(path);
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new OpenCodeHttpException(200, "JSON parse error: " + e.getMessage());
        }
    }

    private String getRaw(String path) {
        String url = config.getServerUrl() + path;
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        try (CloseableHttpResponse response = http.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : "";
            if (status < 200 || status >= 300) {
                throw new OpenCodeHttpException(status, body);
            }
            return body;
        } catch (OpenCodeHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenCodeHttpException(0, "Request failed: " + e.getMessage());
        }
    }

    private <T> T post(String path, Object body, Class<T> type) {
        String json = postRaw(path, body);
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new OpenCodeHttpException(200, "JSON parse error: " + e.getMessage());
        }
    }

    private boolean postNoContent(String path, Object body) {
        String url = config.getServerUrl() + path;
        HttpPost request = new HttpPost(url);
        addAuthHeader(request);
        if (body != null) {
            try {
                String json = MAPPER.writeValueAsString(body);
                request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            } catch (Exception e) {
                throw new OpenCodeHttpException(0, "JSON serialize error: " + e.getMessage());
            }
        }
        try (CloseableHttpResponse response = http.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            return status >= 200 && status < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private String postRaw(String path, Object body) {
        String url = config.getServerUrl() + path;
        HttpPost request = new HttpPost(url);
        addAuthHeader(request);
        if (body != null) {
            try {
                String json = MAPPER.writeValueAsString(body);
                request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            } catch (Exception e) {
                throw new OpenCodeHttpException(0, "JSON serialize error: " + e.getMessage());
            }
        }
        try (CloseableHttpResponse response = http.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : "";
            if (status < 200 || status >= 300) {
                throw new OpenCodeHttpException(status, responseBody);
            }
            return responseBody;
        } catch (OpenCodeHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenCodeHttpException(0, "Request failed: " + e.getMessage());
        }
    }

    private boolean delete(String path) {
        String url = config.getServerUrl() + path;
        HttpDelete request = new HttpDelete(url);
        addAuthHeader(request);
        try (CloseableHttpResponse response = http.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            return status >= 200 && status < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private void addAuthHeader(HttpRequestBase request) {
        String password = config.resolvePassword();
        if (!password.isEmpty()) {
            String credentials = Base64.getEncoder()
                    .encodeToString((config.getUsername() + ":" + password).getBytes());
            request.setHeader("Authorization", "Basic " + credentials);
        }
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new OpenCodeHttpException(200, "JSON parse error: " + e.getMessage());
        }
    }

    private CloseableHttpClient buildHttpClient(OpenCodeClientConfig config) {
        try {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(config.getConnectTimeoutMillis())
                    .setSocketTimeout(config.getReadTimeoutMillis())
                    .setConnectionRequestTimeout(config.getConnectTimeoutMillis())
                    .build();

            if (!config.isVerifySsl()) {
                SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, (chain, authType) -> true)
                        .build();
                SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(
                        sslContext, NoopHostnameVerifier.INSTANCE);
                return HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .setSSLSocketFactory(sslFactory)
                        .build();
            }
            return HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build HTTP client", e);
        }
    }

    @Override
    public void close() {
        try {
            http.close();
        } catch (Exception ignored) {
        }
    }
}
