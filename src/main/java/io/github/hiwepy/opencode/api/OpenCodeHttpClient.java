package io.github.hiwepy.opencode.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.OpenCodeClientConfig;
import io.github.hiwepy.opencode.api.model.*;
import io.github.hiwepy.opencode.exception.OpenCodeHttpException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * OpenCode Server HTTP 客户端，封装 REST API。
 * <p>基于 OkHttp，支持外部传入 {@link OkHttpClient}（复用别的插件实例）。</p>
 *
 * @see <a href="https://opencode.ai/docs/server/">opencode server docs</a>
 */
@Slf4j
public class OpenCodeHttpClient implements AutoCloseable {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OpenCodeClientConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenCodeHttpClient(OpenCodeClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.objectMapper = Objects.isNull(objectMapper) ? new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false): objectMapper;
        this.httpClient = Objects.isNull(httpClient) ? buildOkHttpClient(config) : httpClient;
    }

    private static OkHttpClient buildOkHttpClient(OpenCodeClientConfig config) {
        // 兜底创建
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeoutMillis(), TimeUnit.MILLISECONDS);
        if (!config.isVerifySsl()) {
            builder.hostnameVerifier((hostname, session) -> true);
        }
        return builder.build();
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
        Map<String, Object> body = title != null ? Collections.singletonMap("title", title) : Collections.emptyMap();
        return post("/session", body, Session.class);
    }

    public Session getSession(String sessionId) {
        return get("/session/" + sessionId, Session.class);
    }

    public List<Session> listSessions() {
        return getList("/session", new TypeReference<List<Session>>() {});
    }

    /**
     * 分页/过滤列出 sessions，对齐 Hermes {@code listSessions(limit, offset, source, includeChildren)}。
     *
     * @param search 服务端关键字过滤，为 null 则不过滤
     * @param limit  最大返回条数，为 null 则不限制
     * @param start  分页偏移量，为 null 则从 0 开始
     * @return 匹配的 session 列表
     */
    public List<Session> listSessions(String search, Integer limit, Integer start) {
        HttpUrl.Builder urlBuilder = HttpUrl.get(url("/session")).newBuilder();
        if (search != null) urlBuilder.addQueryParameter("search", search);
        if (limit != null) urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        if (start != null) urlBuilder.addQueryParameter("start", String.valueOf(start));
        Request request = authedRequest(urlBuilder.build().toString()).get().build();
        return executeList(request, new TypeReference<List<Session>>() {});
    }

    /**
     * 按 title 精确查找 session。
     */
    public Optional<Session> findSessionByTitle(String title) {
        if (title == null || title.isEmpty()) {
            return Optional.empty();
        }
        return listSessions(title, 50, null).stream()
                .filter(s -> Objects.equals(title, s.getTitle()))
                .findFirst();
    }

    public boolean deleteSession(String sessionId) {
        Request request = new Request.Builder().url(url("/session/" + sessionId))
                .delete().build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new OpenCodeHttpException("DELETE failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // Message / Prompt
    // ============================================================

    public PromptResult prompt(String sessionId, PromptRequest request) {
        return post("/session/" + sessionId + "/message", request, PromptResult.class);
    }

    public PromptResult chatCompletionWithSession(PromptRequest request, String sessionKey) {
        String sessionId = ensureSession(sessionKey);
        return prompt(sessionId, request);
    }

    public boolean chatCompletionWithSessionAsync(PromptRequest request, String sessionKey) {
        String sessionId = ensureSession(sessionKey);
        return promptAsync(sessionId, request);
    }

    public String ensureSession(String sessionKey) {
        try {
            Optional<Session> existing = findSessionByTitle(sessionKey);
            if (existing.isPresent()) {
                return existing.get().getId();
            }
        } catch (Exception e) {
            log.debug("findSessionByTitle failed, sessionKey={}, error={}", sessionKey, e.getMessage());
        }
        Session session = createSession(sessionKey);
        return session.getId();
    }

    public boolean promptAsync(String sessionId, PromptRequest request) {
        try {
            RequestBody body = RequestBody.create(objectMapper.writeValueAsBytes(request), JSON);
            Request httpReq = new Request.Builder().url(url("/session/" + sessionId + "/prompt_async"))
                    .post(body).build();
            try (Response response = httpClient.newCall(httpReq).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            throw new OpenCodeHttpException("promptAsync failed: " + e.getMessage(), e);
        }
    }

    public List<PromptResult> getMessages(String sessionId) {
        return getList("/session/" + sessionId + "/message", new TypeReference<List<PromptResult>>() {});
    }

    public boolean abortSession(String sessionId) {
        Request request = new Request.Builder().url(url("/session/" + sessionId + "/abort"))
                .post(RequestBody.create(new byte[0], null)).build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new OpenCodeHttpException("abort failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // Agent
    // ============================================================

    public List<Agent> listAgents() {
        return getList("/agent", new TypeReference<List<Agent>>() {});
    }

    /**
     * 暴露 OkHttpClient 供 SSE 客户端复用。
     */
    public OkHttpClient getOkHttpClient() {
        return httpClient;
    }

    /**
     * 暴露 ObjectMapper 供转换器复用。
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private String url(String path) {
        return config.getServerUrl() + path;
    }

    private Request.Builder authedRequest(String url) {
        Request.Builder builder = new Request.Builder().url(url);
        String password = config.resolvePassword();
        if (!password.isEmpty()) {
            String credential = Credentials.basic(config.getUsername(), password);
            builder.header("Authorization", credential);
        }
        return builder;
    }

    private <T> T get(String path, Class<T> type) {
        Request request = authedRequest(url(path)).get().build();
        return execute(request, type);
    }


    private <T> T getList(String path, TypeReference<T> typeRef) {
        Request request = authedRequest(url(path)).get().build();
        return executeList(request, typeRef);
    }


    private <T> T post(String path, Object body, Class<T> type) {
        Request request = authedRequest(url(path))
                .post(RequestBody.create(toJson(body), JSON))
                .build();
        return execute(request, type);
    }

    private <T> T execute(Request request, Class<T> type) {
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new OpenCodeHttpException(response.code(), respBody);
            }
            return objectMapper.readValue(respBody, type);
        } catch (IOException e) {
            throw new OpenCodeHttpException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private <T> T executeList(Request request, TypeReference<T> typeRef) {
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new OpenCodeHttpException(response.code(), respBody);
            }
            return objectMapper.readValue(respBody, typeRef);
        } catch (IOException e) {
            throw new OpenCodeHttpException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new OpenCodeHttpException("Failed to serialize request body: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        // 外部传入的 OkHttpClient 不关闭；自建的也不主动关闭（OkHttpClient 内部管理连接池）
    }
}
