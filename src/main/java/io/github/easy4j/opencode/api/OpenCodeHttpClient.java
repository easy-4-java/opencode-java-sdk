package io.github.easy4j.opencode.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.HttpCallCancellation;
import io.github.easy4j.opencode.OpenCodeOkHttpClientFactory;
import io.github.easy4j.opencode.api.model.*;
import io.github.easy4j.opencode.exception.OpenCodeHttpException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import okio.Buffer;

/**
 * HTTP client for the OpenCode Server REST API.
 *
 * <p>Built on OkHttp; supports externally provided {@link OkHttpClient} instances for
 * connection pooling across plugins. All methods throw {@link io.github.easy4j.opencode.exception.OpenCodeHttpException}
 * on HTTP errors.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see OpenCodeHttpClientConfig
 * @see io.github.easy4j.opencode.exception.OpenCodeHttpException
 * @see <a href="https://opencode.ai/docs/server/">opencode server docs</a>
 */
@Slf4j
public class OpenCodeHttpClient implements AutoCloseable {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String HEADER_OPENCODE_DIRECTORY = "X-OpenCode-Directory";
    private static final AtomicLong REQUEST_SEQUENCE = new AtomicLong();

    private final OpenCodeHttpClientConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenCodeHttpClient(OpenCodeHttpClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.objectMapper = Objects.isNull(objectMapper) ? new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false): objectMapper;
        this.httpClient = Objects.isNull(httpClient) ? buildOkHttpClient(config) : httpClient;
        log.debug("OpenCode HTTP client initialized: baseUrl={}, connectTimeoutMs={}, readTimeoutMs={}, "
                        + "callTimeoutMs={}, retryOnConnectionFailure={}, detailedLoggingEnabled={}",
                config.getBaseUrl(), config.getConnectTimeoutMillis(), config.getReadTimeoutMillis(),
                config.getCallTimeoutMillis(), config.isRetryOnConnectionFailure(),
                config.isDetailedLoggingEnabled());
    }

    private static OkHttpClient buildOkHttpClient(OpenCodeHttpClientConfig config) {
        return OpenCodeOkHttpClientFactory.create(config);
    }

    // ============================================================
    // Global
    // ============================================================

    public HealthStatus health() {
        return awaitFuture(healthAsync());
    }

    /** 异步检查 OpenCode Server 健康状态。 */
    public CompletableFuture<HealthStatus> healthAsync() {
        return getAsync("/global/health", HealthStatus.class);
    }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) {
        return createSession(title, null);
    }

    public Session createSession(String title, OpenCodeRequestContext context) {
        return createSession(title, context, null);
    }

    public Session createSession(String title, OpenCodeRequestContext context,
                                 HttpCallCancellation cancellation) {
        return awaitFuture(createSessionAsync(title, context, cancellation));
    }

    /** 异步创建会话。 */
    public CompletableFuture<Session> createSessionAsync(String title, OpenCodeRequestContext context,
                                                         HttpCallCancellation cancellation) {
        Map<String, Object> body = title != null ? Collections.singletonMap("title", title) : Collections.emptyMap();
        return postAsync("/session", body, Session.class, context, cancellation);
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
        return listSessions(search, limit, start, null);
    }

    public List<Session> listSessions(String search, Integer limit, Integer start,
                                      OpenCodeRequestContext context) {
        return listSessions(search, limit, start, context, null);
    }

    private List<Session> listSessions(String search, Integer limit, Integer start,
                                       OpenCodeRequestContext context,
                                       HttpCallCancellation cancellation) {
        return awaitFuture(listSessionsAsync(search, limit, start, context, cancellation));
    }

    /** 异步分页列出会话。 */
    public CompletableFuture<List<Session>> listSessionsAsync(String search, Integer limit, Integer start,
                                                              OpenCodeRequestContext context,
                                                              HttpCallCancellation cancellation) {
        Map<String, String> params = new HashMap<>();
        if (search != null) params.put("search", search);
        if (limit != null) params.put("limit", String.valueOf(limit));
        if (start != null) params.put("start", String.valueOf(start));
        // context-aware path: rebuild authedRequest with header, then call getWithQuery equivalent
        HttpUrl.Builder urlBuilder = HttpUrl.get(url("/session")).newBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        Request request = authedRequest(urlBuilder.build().toString(), context).get().build();
        return executeListAsync(request, new TypeReference<List<Session>>() {}, cancellation);
    }

    /**
     * 按 title 精确查找 session。
     */
    public Optional<Session> findSessionByTitle(String title) {
        return findSessionByTitle(title, null);
    }

    public Optional<Session> findSessionByTitle(String title, OpenCodeRequestContext context) {
        return findSessionByTitle(title, context, null);
    }

    public Optional<Session> findSessionByTitle(String title, OpenCodeRequestContext context,
                                                HttpCallCancellation cancellation) {
        if (title == null || title.isEmpty()) {
            return Optional.empty();
        }
        return listSessions(title, 50, null, context, cancellation).stream()
                .filter(s -> Objects.equals(title, s.getTitle()))
                .findFirst();
    }

    public boolean deleteSession(String sessionId) {
        return delete("/session/" + sessionId);
    }

    // ============================================================
    // Message / Prompt
    // ============================================================

    public PromptResult prompt(String sessionId, PromptRequest request) {
        return post("/session/" + sessionId + "/message", request, PromptResult.class);
    }

    public PromptResult prompt(String sessionId, PromptRequest request,
                               HttpCallCancellation cancellation) {
        return awaitFuture(promptCompletionAsync(sessionId, request, cancellation));
    }

    /** 异步发送消息并等待完整 Prompt 结果。 */
    public CompletableFuture<PromptResult> promptCompletionAsync(String sessionId, PromptRequest request,
                                                                 HttpCallCancellation cancellation) {
        return postAsync("/session/" + sessionId + "/message", request, PromptResult.class,
                null, cancellation);
    }

    public PromptResult chatCompletionWithSession(PromptRequest request, String sessionKey) {
        String sessionId = ensureSession(sessionKey);
        return prompt(sessionId, request);
    }

    public PromptResult chatCompletionWithSession(PromptRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        String sessionId = ensureSession(sessionKey, null, cancellation);
        return prompt(sessionId, request, cancellation);
    }

    public CompletableFuture<Boolean> chatCompletionWithSessionAsync(PromptRequest request, String sessionKey) {
        return ensureSessionAsync(sessionKey, null, null)
                .thenCompose(sessionId -> promptAsync(sessionId, request));
    }

    public String ensureSession(String sessionKey) {
        return ensureSession(sessionKey, null);
    }

    public String ensureSession(String sessionKey, OpenCodeRequestContext context) {
        return ensureSession(sessionKey, context, null);
    }

    public String ensureSession(String sessionKey, OpenCodeRequestContext context,
                                HttpCallCancellation cancellation) {
        return awaitFuture(ensureSessionAsync(sessionKey, context, cancellation));
    }

    /** 异步查找或创建会话。 */
    public CompletableFuture<String> ensureSessionAsync(String sessionKey, OpenCodeRequestContext context,
                                                        HttpCallCancellation cancellation) {
        return listSessionsAsync(sessionKey, 50, null, context, cancellation)
                .handle((sessions, error) -> {
                    if (Objects.nonNull(error)) {
                        if (Objects.nonNull(cancellation) && cancellation.isCancelled()) {
                            Throwable cause = error instanceof CompletionException && Objects.nonNull(error.getCause())
                                    ? error.getCause() : error;
                            if (cause instanceof RuntimeException) {
                                throw (RuntimeException) cause;
                            }
                            throw new CompletionException(cause);
                        }
                        log.debug("findSessionByTitle failed, sessionKey={}, error={}",
                                sessionKey, error.getMessage());
                        return Optional.<Session>empty();
                    }
                    return sessions.stream().filter(session -> Objects.equals(sessionKey, session.getTitle()))
                            .findFirst();
                }).thenCompose(existing -> existing.isPresent()
                        ? CompletableFuture.completedFuture(existing.get().getId())
                        : createSessionAsync(sessionKey, context, cancellation).thenApply(Session::getId));
    }

    public CompletableFuture<Boolean> promptAsync(String sessionId, PromptRequest request) {
        return promptAsync(sessionId, request, null);
    }

    public CompletableFuture<Boolean> promptAsync(String sessionId, PromptRequest request,
                                                  OpenCodeRequestContext context) {
        try {
            RequestBody body = RequestBody.create(objectMapper.writeValueAsBytes(request), JSON);
            Request httpReq = authedRequest(url("/session/" + sessionId + "/prompt_async"), context)
                    .post(body).build();
            return executeSuccessAsync(httpReq, null);
        } catch (IOException e) {
            return failedFuture(new OpenCodeHttpException("promptAsync failed: " + e.getMessage(), e));
        }
    }

    public List<PromptResult> getMessages(String sessionId) {
        return getList("/session/" + sessionId + "/message", new TypeReference<List<PromptResult>>() {});
    }

    public boolean abortSession(String sessionId) {
        return postNoBody("/session/" + sessionId + "/abort");
    }

    // ============================================================
    // Agent
    // ============================================================

    public List<Agent> listAgents() {
        return getList("/agent", new TypeReference<List<Agent>>() {});
    }

    // ============================================================
    // Config
    // ============================================================

    /**
     * {@code GET /config}，获取实例生效的配置。
     */
    public OpenCodeConfig getConfig() {
        return get("/config", OpenCodeConfig.class);
    }

    /**
     * {@code GET /global/config}，获取全局配置。
     */
    public OpenCodeConfig getGlobalConfig() {
        return get("/global/config", OpenCodeConfig.class);
    }

    /**
     * {@code PATCH /config}，更新实例配置。
     *
     * @param body 配置增量（任何字段均可选）
     */
    public OpenCodeConfig updateConfig(Object body) {
        return patch("/config", body, OpenCodeConfig.class);
    }

    /**
     * {@code PATCH /global/config}，更新全局配置。
     */
    public OpenCodeConfig updateGlobalConfig(Object body) {
        return patch("/global/config", body, OpenCodeConfig.class);
    }

    /**
     * {@code GET /config/providers}，获取已配置的 providers 列表与默认模型映射。
     */
    public ProviderList getConfigProviders() {
        return get("/config/providers", ProviderList.class);
    }

    // ============================================================
    // Project
    // ============================================================

    /**
     * {@code GET /project}，列出所有已知项目。
     */
    public List<Project> listProjects() {
        return getList("/project", new TypeReference<List<Project>>() {});
    }

    /**
     * {@code GET /project/current}，获取当前项目。
     */
    public Project getCurrentProject() {
        return get("/project/current", Project.class);
    }

    /**
     * {@code PATCH /project/:id}，更新项目（name / icon / commands 等）。
     */
    public Project updateProject(String projectId, Object body) {
        return patch("/project/" + projectId, body, Project.class);
    }

    /**
     * {@code POST /project/git/init}，为当前项目初始化 git 仓库。
     */
    public boolean initProjectGit() {
        return postNoBody("/project/git/init");
    }

    // ============================================================
    // Provider
    // ============================================================

    /**
     * {@code GET /provider}，获取 providers 列表 + 默认 provider/model 映射 + 已连接 provider。
     */
    public ProviderList listProviders() {
        return get("/provider", ProviderList.class);
    }

    /**
     * {@code GET /provider/auth}，获取每个 provider 的可用认证方式。
     */
    public Map<String, List<ProviderAuthMethod>> listProviderAuthMethods() {
        Request request = authedRequest(url("/provider/auth")).get().build();
        return executeList(request, new TypeReference<Map<String, List<ProviderAuthMethod>>>() {});
    }

    /**
     * {@code POST /provider/:id/oauth/authorize}，启动 provider OAuth 授权。
     *
     * @param providerId provider ID
     * @param method     认证方式 label（可选）
     */
    public ProviderAuthAuthorization providerOAuthAuthorize(String providerId, String method) {
        Map<String, Object> body = new HashMap<>();
        if (method != null) {
            body.put("method", method);
        }
        return post("/provider/" + providerId + "/oauth/authorize", body,
                ProviderAuthAuthorization.class);
    }

    /**
     * {@code POST /provider/:id/oauth/callback}，处理 OAuth 回调。
     */
    public boolean providerOAuthCallback(String providerId, String code) {
        return awaitFuture(providerOAuthCallbackAsync(providerId, code));
    }

    /** 异步处理 provider OAuth 回调。 */
    public CompletableFuture<Boolean> providerOAuthCallbackAsync(String providerId, String code) {
        Map<String, Object> body = new HashMap<>();
        if (code != null) {
            body.put("code", code);
        }
        Request request = authedRequest(url("/provider/" + providerId + "/oauth/callback"))
                .post(RequestBody.create(toJson(body), JSON)).build();
        return executeSuccessAsync(request, null);
    }

    // ============================================================
    // File / Find
    // ============================================================

    /**
     * {@code GET /file}，列出指定目录下的文件/目录树。
     */
    public List<FileNode> listFiles(String path) {
        return getWithQuery("/file",
                path != null ? Collections.singletonMap("path", path) : null,
                new TypeReference<List<FileNode>>() {});
    }

    /**
     * {@code GET /file/content}，读取文件内容。
     */
    public FileContent getFileContent(String path) {
        return getWithQuery("/file/content",
                path != null ? Collections.singletonMap("path", path) : null,
                FileContent.class);
    }

    /**
     * {@code GET /file/status}，获取项目所有文件的 git 状态。
     */
    public List<FileNode> getFileStatus() {
        return getList("/file/status", new TypeReference<List<FileNode>>() {});
    }

    /**
     * {@code GET /find}，ripgrep 文本搜索。
     */
    public List<FileSearchResult> find(String pattern) {
        return getWithQuery("/find",
                pattern != null ? Collections.singletonMap("pattern", pattern) : null,
                new TypeReference<List<FileSearchResult>>() {});
    }

    /**
     * {@code GET /find/file}，按文件名/模式查找文件。
     */
    public List<String> findFiles(String query) {
        return getWithQuery("/find/file",
                query != null ? Collections.singletonMap("query", query) : null,
                new TypeReference<List<String>>() {});
    }

    /**
     * {@code GET /find/symbol}，通过 LSP 搜索工作区符号。
     */
    public List<Symbol> findSymbols(String query) {
        return getWithQuery("/find/symbol",
                query != null ? Collections.singletonMap("query", query) : null,
                new TypeReference<List<Symbol>>() {});
    }

    // ============================================================
    // Instance / Misc
    // ============================================================

    /**
     * {@code GET /command}，列出已注册的 slash commands。
     */
    public List<Command> listCommands() {
        return getList("/command", new TypeReference<List<Command>>() {});
    }

    /**
     * {@code GET /skill}，列出已注册的 skills。
     */
    public List<Skill> listSkills() {
        return getList("/skill", new TypeReference<List<Skill>>() {});
    }

    /**
     * {@code GET /formatter}，列出 formatter 状态。
     */
    public List<FormatterStatus> listFormatters() {
        return getList("/formatter", new TypeReference<List<FormatterStatus>>() {});
    }

    /**
     * {@code GET /lsp}，列出 LSP 服务器状态。
     */
    public List<LspStatus> listLsps() {
        return getList("/lsp", new TypeReference<List<LspStatus>>() {});
    }

    /**
     * {@code GET /mcp}，列出 MCP 服务器状态。
     */
    public Map<String, McpStatus> listMcpServers() {
        Request request = authedRequest(url("/mcp")).get().build();
        return executeList(request, new TypeReference<Map<String, McpStatus>>() {});
    }

    /**
     * {@code POST /mcp}，动态添加 MCP server。
     */
    public McpStatus addMcpServer(String name, Object config) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("config", config);
        return post("/mcp", body, McpStatus.class);
    }

    /**
     * {@code GET /path}，获取 opencode 工作目录相关的所有路径。
     */
    public OpenCodePath getPath() {
        return get("/path", OpenCodePath.class);
    }

    /**
     * {@code GET /vcs}，获取当前工作目录的 VCS 信息。
     */
    public VcsInfo getVcs() {
        return get("/vcs", VcsInfo.class);
    }

    // ============================================================
    // Session extended
    // ============================================================

    /**
     * {@code GET /session/status}，获取所有 session 状态。
     */
    public Map<String, SessionStatus> getSessionStatusMap() {
        Request request = authedRequest(url("/session/status")).get().build();
        return executeList(request, new TypeReference<Map<String, SessionStatus>>() {});
    }

    /**
     * {@code GET /session/:id/children}，列出 session 的子（forked）session。
     */
    public List<Session> getSessionChildren(String sessionId) {
        return getList("/session/" + sessionId + "/children", new TypeReference<List<Session>>() {});
    }

    /**
     * {@code GET /session/:id/todo}，获取 session 的 todo 列表。
     */
    public List<SessionTodo> getSessionTodo(String sessionId) {
        return getList("/session/" + sessionId + "/todo", new TypeReference<List<SessionTodo>>() {});
    }

    /**
     * {@code GET /session/:id/diff}，获取指定 message 之后的文件 diff。
     */
    public List<FileDiff> getSessionDiff(String sessionId, String messageId) {
        return getWithQuery("/session/" + sessionId + "/diff",
                messageId != null ? Collections.singletonMap("messageID", messageId) : null,
                new TypeReference<List<FileDiff>>() {});
    }

    /**
     * {@code POST /session/:id/share}，创建可分享链接。
     */
    public Session shareSession(String sessionId) {
        return post("/session/" + sessionId + "/share", Collections.emptyMap(), Session.class);
    }

    /**
     * {@code DELETE /session/:id/share}，取消分享。
     */
    public Session unshareSession(String sessionId) {
        return deleteWithResp("/session/" + sessionId + "/share", Session.class);
    }

    /**
     * {@code POST /session/:id/fork}，从指定 message 处 fork session。
     */
    public Session forkSession(String sessionId, String messageId) {
        Map<String, Object> body = new HashMap<>();
        if (messageId != null) {
            body.put("messageID", messageId);
        }
        return post("/session/" + sessionId + "/fork", body, Session.class);
    }

    /**
     * {@code POST /session/:id/init}，用首个 message 初始化 session（创建 AGENTS.md）。
     */
    public boolean initSession(String sessionId, String messageId, String providerId, String modelId) {
        Map<String, Object> body = new HashMap<>();
        body.put("messageID", messageId);
        body.put("providerID", providerId);
        body.put("modelID", modelId);
        return postNoBodyResp("/session/" + sessionId + "/init", body);
    }

    /**
     * {@code POST /session/:id/summarize}，对 session 进行 AI 摘要压缩。
     */
    public boolean summarizeSession(String sessionId, String providerId, String modelId) {
        Map<String, Object> body = new HashMap<>();
        body.put("providerID", providerId);
        body.put("modelID", modelId);
        return postNoBodyResp("/session/" + sessionId + "/summarize", body);
    }

    /**
     * {@code POST /session/:id/revert}，回退到指定 message（可指定 part）。
     */
    public boolean revertSession(String sessionId, String messageId, String partId) {
        Map<String, Object> body = new HashMap<>();
        body.put("messageID", messageId);
        if (partId != null) {
            body.put("partID", partId);
        }
        return postNoBodyResp("/session/" + sessionId + "/revert", body);
    }

    /**
     * {@code POST /session/:id/unrevert}，撤销回退。
     */
    public boolean unrevertSession(String sessionId) {
        return postNoBody("/session/" + sessionId + "/unrevert");
    }

    /**
     * {@code GET /session/:id/message/:messageID}，获取单条 message 详情。
     */
    public MessageInfo getMessage(String sessionId, String messageId) {
        return get("/session/" + sessionId + "/message/" + messageId, MessageInfo.class);
    }

    /**
     * {@code POST /session/:id/command}，向 session 发送 slash command。
     */
    public PromptResult runSessionCommand(String sessionId, String command, String arguments,
                                          String agent, String model) {
        Map<String, Object> body = new HashMap<>();
        body.put("command", command);
        if (arguments != null) {
            body.put("arguments", arguments);
        }
        if (agent != null) {
            body.put("agent", agent);
        }
        if (model != null) {
            body.put("model", model);
        }
        return post("/session/" + sessionId + "/command", body, PromptResult.class);
    }

    // ============================================================
    // Question
    // ============================================================

    /**
     * {@code GET /question}，列出所有待回答的问题。
     */
    public List<QuestionRequest> listQuestions() {
        return getList("/question", new TypeReference<List<QuestionRequest>>() {});
    }

    /**
     * {@code POST /question/:id/reply}，回复问题。
     */
    public boolean replyQuestion(String requestId, List<String> answers) {
        Map<String, Object> body = new HashMap<>();
        body.put("answers", answers);
        return postNoBodyResp("/question/" + requestId + "/reply", body);
    }

    /**
     * {@code POST /question/:id/reject}，拒绝回答问题。
     */
    public boolean rejectQuestion(String requestId) {
        return postNoBody("/question/" + requestId + "/reject");
    }

    // ============================================================
    // Permission
    // ============================================================

    /**
     * {@code GET /permission}，列出所有待审批权限请求。
     */
    public List<PermissionRequest> listPermissions() {
        return getList("/permission", new TypeReference<List<PermissionRequest>>() {});
    }

    /**
     * {@code POST /permission/:id/reply}，回复权限请求。
     *
     * @param requestId 请求 ID
     * @param response  "approve" / "deny" / "always"
     * @param remember  是否记住该决策
     */
    public boolean replyPermission(String requestId, String response, boolean remember) {
        Map<String, Object> body = new HashMap<>();
        body.put("response", response);
        body.put("remember", remember);
        return postNoBodyResp("/permission/" + requestId + "/reply", body);
    }

    // ============================================================
    // Auth
    // ============================================================

    /**
     * {@code PUT /auth/:id}，设置 provider 凭证。
     */
    public boolean setAuth(String providerId, Object body) {
        return awaitFuture(setAuthAsync(providerId, body));
    }

    /** 异步设置 provider 凭证。 */
    public CompletableFuture<Boolean> setAuthAsync(String providerId, Object body) {
        Request request = authedRequest(url("/auth/" + providerId))
                .put(RequestBody.create(toJson(body), JSON)).build();
        return executeSuccessAsync(request, null);
    }

    /**
     * {@code DELETE /auth/:id}，清除 provider 凭证。
     */
    public boolean removeAuth(String providerId) {
        return delete("/auth/" + providerId);
    }

    // ============================================================
    // Instance / Global lifecycle
    // ============================================================

    /**
     * {@code POST /instance/dispose}，释放当前 instance。
     */
    public boolean disposeInstance() {
        return postNoBody("/instance/dispose");
    }

    /**
     * {@code POST /global/dispose}，释放所有 instance。
     */
    public boolean globalDispose() {
        return postNoBody("/global/dispose");
    }

    /**
     * {@code POST /global/upgrade}，升级 opencode。{@code target} 为 null 时升级到最新。
     */
    public boolean globalUpgrade(String target) {
        Map<String, Object> body = new HashMap<>();
        if (target != null) {
            body.put("target", target);
        }
        return postNoBodyResp("/global/upgrade", body);
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
        return config.getBaseUrl() + path;
    }

    private Request.Builder authedRequest(String url) {
        return authedRequest(url, null);
    }

    private Request.Builder authedRequest(String url, OpenCodeRequestContext context) {
        Request.Builder builder = new Request.Builder().url(url);
        String password = config.resolvePassword();
        if (!password.isEmpty()) {
            String credential = Credentials.basic(config.getUsername(), password);
            builder.header("Authorization", credential);
        }
        if (Objects.nonNull(context) && context.getDirectory() != null
                && !context.getDirectory().trim().isEmpty()) {
            builder.header(HEADER_OPENCODE_DIRECTORY, context.getDirectory());
        }
        return builder;
    }

    private <T> T get(String path, Class<T> type) {
        return awaitFuture(getAsync(path, type));
    }

    private <T> CompletableFuture<T> getAsync(String path, Class<T> type) {
        Request request = authedRequest(url(path)).get().build();
        return executeAsync(request, type, null);
    }


    private <T> T getList(String path, TypeReference<T> typeRef) {
        Request request = authedRequest(url(path)).get().build();
        return executeList(request, typeRef);
    }

    private <T> T getWithQuery(String path, Map<String, String> queryParams, Class<T> type) {
        HttpUrl.Builder urlBuilder = HttpUrl.get(url(path)).newBuilder();
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        Request request = authedRequest(urlBuilder.build().toString()).get().build();
        return execute(request, type);
    }

    private <T> T getWithQuery(String path, Map<String, String> queryParams, TypeReference<T> typeRef) {
        HttpUrl.Builder urlBuilder = HttpUrl.get(url(path)).newBuilder();
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        Request request = authedRequest(urlBuilder.build().toString()).get().build();
        return executeList(request, typeRef);
    }


    private <T> T post(String path, Object body, Class<T> type) {
        return post(path, body, type, null);
    }

    private <T> T post(String path, Object body, Class<T> type, OpenCodeRequestContext context) {
        return post(path, body, type, context, null);
    }

    private <T> T post(String path, Object body, Class<T> type, OpenCodeRequestContext context,
                       HttpCallCancellation cancellation) {
        return awaitFuture(postAsync(path, body, type, context, cancellation));
    }

    private <T> CompletableFuture<T> postAsync(String path, Object body, Class<T> type,
                                               OpenCodeRequestContext context,
                                               HttpCallCancellation cancellation) {
        Request request = authedRequest(url(path), context)
                .post(RequestBody.create(toJson(body), JSON))
                .build();
        return executeAsync(request, type, cancellation);
    }

    private <T> T patch(String path, Object body, Class<T> type) {
        return patch(path, body, type, null);
    }

    private <T> T patch(String path, Object body, Class<T> type, OpenCodeRequestContext context) {
        Request request = authedRequest(url(path), context)
                .patch(RequestBody.create(toJson(body), JSON))
                .build();
        return execute(request, type);
    }

    private <T> T put(String path, Object body, Class<T> type) {
        return put(path, body, type, null);
    }

    private <T> T put(String path, Object body, Class<T> type, OpenCodeRequestContext context) {
        Request request = authedRequest(url(path), context)
                .put(RequestBody.create(toJson(body), JSON))
                .build();
        return execute(request, type);
    }

    private boolean postNoBody(String path) {
        return postNoBody(path, null);
    }

    private boolean postNoBody(String path, OpenCodeRequestContext context) {
        Request request = authedRequest(url(path), context)
                .post(RequestBody.create(new byte[0], null)).build();
        return awaitFuture(executeSuccessAsync(request, null));
    }

    /**
     * POST 带 JSON body，返回 Boolean（仅判断 2xx）。
     */
    private boolean postNoBodyResp(String path, Object body) {
        Request request = authedRequest(url(path))
                .post(RequestBody.create(toJson(body), JSON)).build();
        return awaitFuture(executeSuccessAsync(request, null));
    }

    /**
     * DELETE，返回反序列化的对象。
     */
    private <T> T deleteWithResp(String path, Class<T> type) {
        Request request = authedRequest(url(path)).delete().build();
        return execute(request, type);
    }

    private boolean delete(String path) {
        return delete(path, null);
    }

    private boolean delete(String path, OpenCodeRequestContext context) {
        Request request = authedRequest(url(path), context).delete().build();
        return awaitFuture(executeSuccessAsync(request, null));
    }

    private <T> T execute(Request request, Class<T> type) {
        return execute(request, type, null);
    }

    private <T> T execute(Request request, Class<T> type, HttpCallCancellation cancellation) {
        return awaitFuture(executeAsync(request, type, cancellation));
    }

    /** 使用 OkHttp enqueue 异步执行并反序列化对象。 */
    protected <T> CompletableFuture<T> executeAsync(Request request, Class<T> type,
                                                    HttpCallCancellation cancellation) {
        return executeResponseAsync(request, cancellation).thenApply(response -> {
            if (!response.isSuccessful()) {
                throw new OpenCodeHttpException(response.getStatusCode(), response.getBody());
            }
            try {
                return objectMapper.readValue(response.getBody(), type);
            } catch (IOException error) {
                throw new OpenCodeHttpException("Failed to parse response: " + error.getMessage(), error);
            }
        });
    }

    private CompletableFuture<HttpResponseData> executeResponseAsync(Request request,
                                                                     HttpCallCancellation cancellation) {
        long requestId = beginTrace(request);
        long startedAt = System.nanoTime();
        Call call = httpClient.newCall(request);
        AutoCloseable registration = Objects.nonNull(cancellation) ? cancellation.onCancel(call::cancel) : null;
        CompletableFuture<HttpResponseData> result = new CompletableFuture<>();
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call ignored, IOException error) {
                closeRegistration(registration);
                logFailure(requestId, request, startedAt, error);
                result.completeExceptionally(new OpenCodeHttpException(
                        "HTTP request failed: " + error.getMessage(), error));
            }

            @Override
            public void onResponse(Call ignored, Response response) {
                try (Response completed = response) {
                    String body = Objects.nonNull(completed.body()) ? completed.body().string() : "";
                    logResponse(requestId, request, completed.code(), body, startedAt);
                    result.complete(new HttpResponseData(completed.code(), body));
                } catch (Exception error) {
                    logFailure(requestId, request, startedAt, error);
                    result.completeExceptionally(error);
                } finally {
                    closeRegistration(registration);
                }
            }
        });
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) {
                call.cancel();
            }
        });
        return result;
    }

    private void closeRegistration(AutoCloseable registration) {
        if (registration == null) {
            return;
        }
        try {
            registration.close();
        } catch (Exception error) {
            log.debug("Failed to unregister HTTP cancellation callback: {}", error.getMessage());
        }
    }

    private <T> T executeList(Request request, TypeReference<T> typeRef) {
        return executeList(request, typeRef, null);
    }

    private <T> T executeList(Request request, TypeReference<T> typeRef,
                              HttpCallCancellation cancellation) {
        return awaitFuture(executeListAsync(request, typeRef, cancellation));
    }

    private <T> CompletableFuture<T> executeListAsync(Request request, TypeReference<T> typeRef,
                                                      HttpCallCancellation cancellation) {
        return executeResponseAsync(request, cancellation).thenApply(response -> {
            if (!response.isSuccessful()) {
                throw new OpenCodeHttpException(response.getStatusCode(), response.getBody());
            }
            try {
                return objectMapper.readValue(response.getBody(), typeRef);
            } catch (IOException error) {
                throw new OpenCodeHttpException("Failed to parse response: " + error.getMessage(), error);
            }
        });
    }

    private CompletableFuture<Boolean> executeSuccessAsync(Request request,
                                                           HttpCallCancellation cancellation) {
        return executeResponseAsync(request, cancellation).thenApply(HttpResponseData::isSuccessful);
    }

    protected <T> T awaitFuture(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException error) {
            Throwable cause = Objects.nonNull(error.getCause()) ? error.getCause() : error;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new OpenCodeHttpException("Async HTTP request failed: " + cause.getMessage(), cause);
        }
    }

    private <T> CompletableFuture<T> failedFuture(Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }

    private static final class HttpResponseData {
        private final int statusCode;
        private final String body;

        private HttpResponseData(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        private int getStatusCode() {
            return statusCode;
        }

        private String getBody() {
            return body;
        }

        private boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    private long beginTrace(Request request) {
        long requestId = REQUEST_SEQUENCE.incrementAndGet();
        log.debug("HTTP request started: requestId={}, method={}, url={}",
                requestId, request.method(), request.url());
        if (config.isDetailedLoggingEnabled()) {
            log.debug("HTTP request details: requestId={}, headers={}, body={}", requestId,
                    redactHeaders(request.headers()), requestBody(request));
        }
        return requestId;
    }

    private void logResponse(long requestId, Request request, int status, String body, long startedAt) {
        log.debug("HTTP request completed: requestId={}, method={}, url={}, status={}, bodyLength={}, elapsedMs={}",
                requestId, request.method(), request.url(), status, body.length(), elapsedMillis(startedAt));
        if (config.isDetailedLoggingEnabled()) {
            log.debug("HTTP response body: requestId={}, body={}", requestId, truncate(body));
        }
    }

    private void logFailure(long requestId, Request request, long startedAt, Exception error) {
        log.warn("HTTP request failed: requestId={}, method={}, url={}, elapsedMs={}, error={}",
                requestId, request.method(), request.url(), elapsedMillis(startedAt), error.getMessage());
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String requestBody(Request request) {
        if (Objects.isNull(request.body())) {
            return "";
        }
        try {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            return truncate(buffer.readUtf8());
        } catch (IOException error) {
            return "<unavailable:" + error.getMessage() + ">";
        }
    }

    private String truncate(String value) {
        int limit = Math.max(0, config.getMaxLoggedBodyLength());
        return value.length() <= limit ? value : value.substring(0, limit) + "...<truncated>";
    }

    private Headers redactHeaders(Headers headers) {
        Headers.Builder safe = headers.newBuilder();
        for (String name : headers.names()) {
            String lowerName = name.toLowerCase();
            if ("authorization".equals(lowerName) || lowerName.contains("token") || lowerName.contains("key")) {
                safe.set(name, "██");
            }
        }
        return safe.build();
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
