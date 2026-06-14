package io.github.hiwepy.opencode.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.OpenCodeClientConfig;
import io.github.hiwepy.opencode.exception.OpenCodeHttpException;
import io.github.hiwepy.opencode.model.*;
import kong.unirest.core.*;
import kong.unirest.modules.jackson.JacksonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * OpenCode Server HTTP 客户端，封装 REST API。
 *
 * @see <a href="https://opencode.ai/docs/server/">opencode server docs</a>
 */
public class OpenCodeHttpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeHttpClient.class);

    private final OpenCodeClientConfig config;
    private final UnirestInstance unirest;

    public OpenCodeHttpClient(OpenCodeClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.unirest = new UnirestInstance(new Config()
                .connectTimeout(config.getConnectTimeoutMillis())
                .requestTimeout(config.getReadTimeoutMillis())
                .verifySsl(config.isVerifySsl())
                .setObjectMapper(new JacksonObjectMapper(mapper)));

        String password = config.resolvePassword();
        if (!password.isEmpty()) {
            this.unirest.config().setDefaultBasicAuth(config.getUsername(), password);
        }
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
        return getList("/session", new GenericType<List<Session>>() {});
    }

    /**
     * 分页/过滤列出 sessions，对齐 Hermes {@code listSessions(limit, offset, source, includeChildren)}。
     * <p>OpenCode Server {@code GET /session} 支持 {@code search}、{@code limit}、{@code start} 查询参数，
     * 避免全量拉取后再内存过滤。</p>
     *
     * @param search 服务端关键字过滤（匹配 title 等），为 null 则不过滤
     * @param limit  最大返回条数，为 null 则不限制
     * @param start  分页偏移量，为 null 则从 0 开始
     * @return 匹配的 session 列表
     */
    public List<Session> listSessions(String search, Integer limit, Integer start) {
        HttpRequest req = unirest.get(url("/session"));
        if (search != null) req.queryString("search", search);
        if (limit != null) req.queryString("limit", limit);
        if (start != null) req.queryString("start", start);
        HttpResponse<List<Session>> resp = req.asObject(new GenericType<List<Session>>() {});
        checkSuccess(resp);
        return resp.getBody();
    }

    /**
     * 按 title 精确查找 session，用于「先找现有 session、找不到再创建」的复用场景。
     * <p>先用 {@code search} 在服务端预过滤（减少传输量），再在客户端精确匹配 title。</p>
     *
     * @param title 期望的 session title
     * @return 命中的第一个 session，未命中返回 {@link Optional#empty()}
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
        HttpResponse<String> resp = unirest.delete(url("/session/" + sessionId)).asString();
        return resp.isSuccess();
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
     * 按 sessionKey 发送消息并同步等待 AI 响应。
     * <p>sessionKey 作为 session 的 title，{@code ensureSession} 保证 session 存在（不存在则创建），
     * 对调用方透明。对齐 Hermes/OpenClaw 的 sessionKey 模式。</p>
     *
     * @param sessionKey 会话复用 key（建议用 {@code OpenCodeSessionKeys} 生成）
     * @param request    prompt 请求
     * @return AI 响应
     */
    public PromptResult promptByKey(String sessionKey, PromptRequest request) {
        String sessionId = ensureSession(sessionKey);
        return prompt(sessionId, request);
    }

    /**
     * 按 sessionKey 异步发送消息，不等待响应。
     */
    public boolean promptAsyncByKey(String sessionKey, PromptRequest request) {
        String sessionId = ensureSession(sessionKey);
        return promptAsync(sessionId, request);
    }

    /**
     * 确保指定 sessionKey 对应的 session 存在，返回其 sessionId。
     * <p>先按 title 精确查找现有 session，不存在则创建。对齐 Hermes/OpenClaw 的 sessionKey 语义。</p>
     *
     * @param sessionKey 会话复用 key（同时作为 session title）
     * @return sessionId
     */
    public String ensureSession(String sessionKey) {
        // 先按 title 查找现有 session
        try {
            Optional<Session> existing = findSessionByTitle(sessionKey);
            if (existing.isPresent()) {
                return existing.get().getId();
            }
        } catch (Exception e) {
            log.debug("findSessionByTitle failed, will create new session, sessionKey={}, error={}",
                    sessionKey, e.getMessage());
        }
        // 不存在则创建
        Session session = createSession(sessionKey);
        return session.getId();
    }

    /**
     * 异步发送消息，不等待响应（POST /session/:id/prompt_async）。
     */
    public boolean promptAsync(String sessionId, PromptRequest request) {
        HttpResponse<String> resp = unirest.post(url("/session/" + sessionId + "/prompt_async"))
                .header("Content-Type", "application/json")
                .body(request)
                .asString();
        return resp.isSuccess();
    }

    public List<PromptResult> getMessages(String sessionId) {
        return getList("/session/" + sessionId + "/message", new GenericType<List<PromptResult>>() {});
    }

    /**
     * 中止正在运行的会话。
     */
    public boolean abortSession(String sessionId) {
        HttpResponse<String> resp = unirest.post(url("/session/" + sessionId + "/abort")).asString();
        return resp.isSuccess();
    }

    // ============================================================
    // Agent
    // ============================================================

    public List<Agent> listAgents() {
        return getList("/agent", new GenericType<List<Agent>>() {});
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private String url(String path) {
        return config.getServerUrl() + path;
    }

    private <T> T get(String path, Class<T> type) {
        HttpResponse<T> resp = unirest.get(url(path)).asObject(type);
        if (!resp.isSuccess()) {
            throw new OpenCodeHttpException(resp.getStatus(),
                    resp.getBody() != null ? resp.getBody().toString() : "");
        }
        return resp.getBody();
    }

    private <T> T getList(String path, GenericType<T> genericType) {
        HttpResponse<T> resp = unirest.get(url(path)).asObject(genericType);
        checkSuccess(resp);
        return resp.getBody();
    }

    private void checkSuccess(HttpResponse<?> resp) {
        if (!resp.isSuccess()) {
            throw new OpenCodeHttpException(resp.getStatus(),
                    resp.getBody() != null ? resp.getBody().toString() : "");
        }
    }

    private <T> T post(String path, Object body, Class<T> type) {
        HttpResponse<T> resp = unirest.post(url(path))
                .header("Content-Type", "application/json")
                .body(body)
                .asObject(type);
        if (!resp.isSuccess()) {
            throw new OpenCodeHttpException(resp.getStatus(),
                    resp.getBody() != null ? resp.getBody().toString() : "");
        }
        return resp.getBody();
    }

    @Override
    public void close() {
        unirest.close();
    }
}
