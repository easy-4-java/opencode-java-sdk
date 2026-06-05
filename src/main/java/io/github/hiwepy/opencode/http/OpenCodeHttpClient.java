package io.github.hiwepy.opencode.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.OpenCodeClientConfig;
import io.github.hiwepy.opencode.exception.OpenCodeHttpException;
import io.github.hiwepy.opencode.model.*;
import kong.unirest.core.*;
import kong.unirest.jackson.JacksonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.Map;

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
        this.config = config;
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.unirest = Unirest.primaryInstance();
        this.unirest.config()
                .baseUrl(config.getServerUrl())
                .connectTimeout(config.getConnectTimeoutMillis())
                .socketTimeout(config.getReadTimeoutMillis())
                .verifySsl(config.isVerifySsl())
                .setObjectMapper(new JacksonObjectMapper(mapper));

        String password = config.resolvePassword();
        if (!password.isEmpty()) {
            String credentials = Base64.getEncoder()
                    .encodeToString((config.getUsername() + ":" + password).getBytes());
            this.unirest.config().addDefaultHeader("Authorization", "Basic " + credentials);
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
        return get("/session", new TypeReference<List<Session>>() {});
    }

    public boolean deleteSession(String sessionId) {
        HttpResponse<String> resp = unirest.delete("/session/" + sessionId).asString();
        return resp.isSuccess();
    }

    // ============================================================
    // Message / Prompt
    // ============================================================

    /**
     * 发送消息并同步等待 AI 响应（POST /session/:id/message）。
     * <p>此方法会阻塞直到 AI 完成响应，可能耗时较长。</p>
     */
    public PromptResult prompt(String sessionId, PromptRequest request) {
        return post("/session/" + sessionId + "/message", request, PromptResult.class);
    }

    /**
     * 异步发送消息，不等待响应（POST /session/:id/prompt_async）。
     * <p>返回 204 No Content 表示已接受。</p>
     *
     * @return 是否成功提交
     */
    public boolean promptAsync(String sessionId, PromptRequest request) {
        HttpResponse<String> resp = unirest.post("/session/" + sessionId + "/prompt_async")
                .body(request)
                .asString();
        return resp.isSuccess();
    }

    public List<PromptResult> getMessages(String sessionId) {
        return get("/session/" + sessionId + "/message", new TypeReference<List<PromptResult>>() {});
    }

    /**
     * 中止正在运行的会话。
     */
    public boolean abortSession(String sessionId) {
        HttpResponse<String> resp = unirest.post("/session/" + sessionId + "/abort").asString();
        return resp.isSuccess();
    }

    // ============================================================
    // Agent
    // ============================================================

    public List<Agent> listAgents() {
        return get("/agent", new TypeReference<List<Agent>>() {});
    }

    // ============================================================
    // MCP
    // ============================================================

    public Map<String, Object> getMcpStatus() {
        return get("/mcp", new TypeReference<Map<String, Object>>() {});
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private <T> T get(String path, Class<T> type) {
        HttpResponse<T> resp = unirest.get(path).asObject(type);
        if (!resp.isSuccess()) {
            throw new OpenCodeHttpException(resp.getStatus(), resp.getBody() != null ? resp.getBody().toString() : "");
        }
        return resp.getBody();
    }

    private <T> T get(String path, TypeReference<T> typeRef) {
        HttpResponse<T> resp = unirest.get(path).asObject(typeRef);
        if (!resp.isSuccess()) {
            throw new OpenCodeHttpException(resp.getStatus(), resp.getBody() != null ? resp.getBody().toString() : "");
        }
        return resp.getBody();
    }

    private <T> T post(String path, Object body, Class<T> type) {
        HttpResponse<T> resp = unirest.post(path)
                .header("Content-Type", "application/json")
                .body(body)
                .asObject(type);
        if (!resp.isSuccess()) {
            throw new OpenCodeHttpException(resp.getStatus(), resp.getBody() != null ? resp.getBody().toString() : "");
        }
        return resp.getBody();
    }

    @Override
    public void close() {
        unirest.shutDown();
    }
}
