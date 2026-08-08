package io.github.easy4j.opencode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.api.mapper.ChatMessageMapper;
import io.github.easy4j.opencode.api.model.*;
import io.github.easy4j.opencode.cli.OpenCodeCli;
import io.github.easy4j.opencode.cli.OpenCodeCliExecutor;
import io.github.easy4j.opencode.cli.availability.OpenCodeCliAvailabilityReport;
import io.github.easy4j.opencode.api.OpenCodeHttpClient;
import io.github.easy4j.opencode.api.OpenCodeChatClient;
import io.github.easy4j.opencode.api.OpenCodeRequestContext;
import io.github.easy4j.opencode.api.OpenCodeSseClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * OpenCode 客户端门面：HTTP Server + SSE 事件流 + 本地 CLI。
 * <p>
 * 三条通信通道相互独立，按各自子配置的 {@code enabled} 决定是否创建：
 * </p>
 * <ul>
 *     <li>{@link OpenCodeHttpClientConfig#isEnabled()} = false → HTTP / SSE 子客户端为 {@code null}</li>
 *     <li>{@link OpenCodeCliConfig#isEnabled()} = false → CLI 子客户端为 {@code null}</li>
 * </ul>
 *
 * <h3>构造器选择</h3>
 * <p>提供 8 个重载覆盖三类场景：</p>
 * <ul>
 *     <li>仅 HTTP / 仅 CLI：传入单个子配置，禁用另一子系统</li>
 *     <li>HTTP + CLI：传入两个子配置，子系统都按各自 {@code enabled} 决定</li>
 *     <li>组合配置：传入 {@link OpenCodeClientConfig}，内部拆分为两个子配置</li>
 * </ul>
 * <p>每种场景再分「自动 ObjectMapper/OkHttpClient」与「强制注入」两个变体。
 * 强制注入的版本对 {@code ObjectMapper}/{@code OkHttpClient} 进行 {@code requireNonNull} 校验。</p>
 *
 * <h3>启动自检</h3>
 * <p>主构造器在子系统初始化后按 {@code startupCheckEnabled} 与 {@code failFastOnUnavailable}
 * 执行健康探测（HTTP：{@code GET /global/health}；CLI：{@code opencode --version}）。
 * 探测失败但未开启 fail-fast 时仅 WARN，不中断构造。</p>
 */
@Slf4j
public class OpenCodeClient implements AutoCloseable {

    private final OpenCodeClientConfig config;
    private final OpenCodeHttpClient httpClient;
    private final OpenCodeChatClient chatClient;
    private final OpenCodeSseClient sseClient;
    private final OpenCodeCli cli;
    private final ExecutorService streamExecutor;

    // ============================================================
    // 构造器
    // ============================================================

    /** 仅 HTTP 子系统（CLI 禁用）。自动创建默认 ObjectMapper 与 OkHttpClient。 */
    public OpenCodeClient(OpenCodeHttpClientConfig httpConfig) {
        this(httpConfig, new OpenCodeCliConfig(), new ObjectMapper(), null);
    }

    /** 仅 HTTP 子系统（CLI 禁用），强制注入共享 ObjectMapper 与 OkHttpClient。 */
    public OpenCodeClient(OpenCodeHttpClientConfig httpConfig, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(httpConfig, new OpenCodeCliConfig(), objectMapper, httpClient);
    }

    /** 仅 CLI 子系统（HTTP 禁用）。自动创建默认 ObjectMapper 与 OkHttpClient。 */
    public OpenCodeClient(OpenCodeCliConfig cliConfig) {
        this(new OpenCodeHttpClientConfig(), cliConfig, new ObjectMapper(), null);
    }

    /** 仅 CLI 子系统（HTTP 禁用），强制注入共享 ObjectMapper 与 OkHttpClient。 */
    public OpenCodeClient(OpenCodeCliConfig cliConfig, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(new OpenCodeHttpClientConfig(), cliConfig, objectMapper, httpClient);
    }

    /** HTTP + CLI 子系统。自动创建默认 ObjectMapper 与 OkHttpClient。 */
    public OpenCodeClient(OpenCodeHttpClientConfig httpConfig, OpenCodeCliConfig cliConfig) {
        this(httpConfig, cliConfig, new ObjectMapper(), null);
    }

    /**
     * HTTP + CLI 子系统，强制注入共享 ObjectMapper 与 OkHttpClient。
     * <p>
     * <b>主构造器</b>：所有参数 {@code requireNonNull}；HTTP/CLI 子客户端按各自
     * {@code enabled} 决定是否创建（禁用时为 {@code null}）；构造完成后按
     * {@code startupCheckEnabled} 与 {@code failFastOnUnavailable} 执行启动自检。
     * </p>
     */
    public OpenCodeClient(OpenCodeHttpClientConfig httpConfig, OpenCodeCliConfig cliConfig,
                          ObjectMapper objectMapper, OkHttpClient httpClient) {
        Objects.requireNonNull(httpConfig, "httpConfig");
        Objects.requireNonNull(cliConfig, "cliConfig");
        Objects.requireNonNull(objectMapper, "objectMapper");

        boolean httpEnabled = httpConfig.isEnabled();
        boolean cliEnabled = cliConfig.isEnabled();

        // 内部聚合配置
        this.config = new OpenCodeClientConfig();
        copyHttpConfig(httpConfig);
        copyCliConfig(cliConfig);

        // HTTP 子系统初始化
        if (httpEnabled) {
            this.chatClient = new OpenCodeChatClient(httpConfig, objectMapper, httpClient);
            this.httpClient = this.chatClient;
            this.sseClient = this.chatClient.events();
        } else {
            this.httpClient = null;
            this.chatClient = null;
            this.sseClient = null;
        }
        this.streamExecutor = createStreamExecutor(httpConfig);

        // CLI 子系统初始化
        if (cliEnabled) {
            this.cli = new OpenCodeCli(new OpenCodeCliExecutor(cliConfig));
        } else {
            this.cli = null;
        }

        // 启动自检
        runStartupChecks(httpConfig, cliConfig);
    }

    /** 组合配置，自动创建默认 ObjectMapper 与 OkHttpClient。 */
    public OpenCodeClient(OpenCodeClientConfig config) {
        this(config, new ObjectMapper(), null);
    }

    /** 组合配置，强制注入共享 ObjectMapper 与 OkHttpClient。 */
    public OpenCodeClient(OpenCodeClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(Objects.requireNonNull(config, "config").getHttp(),
                config.getCli(),
                objectMapper,
                httpClient);
    }

    /**
     * 全量依赖注入（用于测试或自定义组件）。
     * <p>使用此构造方法<b>不会</b>执行任何启动自检。</p>
     */
    public OpenCodeClient(OpenCodeClientConfig config,
                          OpenCodeHttpClient httpClient,
                          OpenCodeSseClient sseClient,
                          OpenCodeCli cli) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = httpClient;
        this.chatClient = httpClient instanceof OpenCodeChatClient ? (OpenCodeChatClient) httpClient : null;
        this.sseClient = sseClient;
        this.cli = cli;
        this.streamExecutor = createStreamExecutor(config.getHttp());
    }

    private static ExecutorService createStreamExecutor(OpenCodeHttpClientConfig config) {
        int corePoolSize = Math.max(1, config.getStreamCorePoolSize());
        int maxPoolSize = Math.max(corePoolSize, config.getStreamMaxPoolSize());
        AtomicInteger threadIndex = new AtomicInteger();
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                Math.max(1L, config.getStreamKeepAliveMillis()), TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(Math.max(1, config.getStreamQueueCapacity())), runnable -> {
                    Thread thread = new Thread(runnable,
                            "opencode-stream-consumer-" + threadIndex.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    // ============================================================
    // 启动自检
    // ============================================================

    private void runStartupChecks(OpenCodeHttpClientConfig httpConfig, OpenCodeCliConfig cliConfig) {
        if (httpConfig.isEnabled() && httpConfig.isStartupCheckEnabled()) {
            try {
                httpClient.health();
                log.info("OpenCode HTTP health check passed: {}", httpConfig.getServerUrl());
            } catch (Exception e) {
                if (httpConfig.isFailFastOnUnavailable()) {
                    throw new IllegalStateException(
                            "OpenCode HTTP service is not available: " + e.getMessage()
                                    + ". Set OpenCodeHttpClientConfig.enabled=false or startupCheckEnabled=false to disable.",
                            e);
                }
                log.warn("OpenCode HTTP service is not available (continuing without strict check): {}", e.getMessage());
            }
        }

        if (cliConfig.isEnabled() && cliConfig.isStartupCheckEnabled()) {
            OpenCodeCliAvailabilityReport report = new io.github.easy4j.opencode.cli.availability.OpenCodeCliAvailabilityChecker().check(cliConfig);
            if (!report.isAvailable()) {
                if (cliConfig.isFailFastOnUnavailable()) {
                    throw new IllegalStateException(
                            "OpenCode CLI is not available: " + report.toDiagnosticMessage()
                                    + ". Set OpenCodeCliConfig.enabled=false or startupCheckEnabled=false to disable.");
                }
                log.warn("OpenCode CLI startup check failed (fail-fast disabled): {}",
                        report.toDiagnosticMessage());
            } else {
                log.info("OpenCode CLI ready: {}", report.toDiagnosticMessage());
            }
        }
    }

    // ============================================================
    // 子系统启用状态查询
    // ============================================================

    /** HTTP 子系统是否启用。 */
    public boolean isHttpEnabled() {
        return httpClient != null;
    }

    /** CLI 子系统是否启用。 */
    public boolean isCliEnabled() {
        return cli != null;
    }

    // ============================================================
    // 配置复制
    // ============================================================

    private void copyHttpConfig(OpenCodeHttpClientConfig src) {
        this.config.getHttp().setMode(src.getMode());
        this.config.getHttp().setEnabled(src.isEnabled());
        this.config.getHttp().setStartupCheckEnabled(src.isStartupCheckEnabled());
        this.config.getHttp().setFailFastOnUnavailable(src.isFailFastOnUnavailable());
        this.config.getHttp().setServerUrl(src.getServerUrl());
        this.config.getHttp().setUsername(src.getUsername());
        this.config.getHttp().setPassword(src.getPassword());
        this.config.getHttp().setConnectTimeoutMillis(src.getConnectTimeoutMillis());
        this.config.getHttp().setReadTimeoutMillis(src.getReadTimeoutMillis());
        this.config.getHttp().setWriteTimeoutMillis(src.getWriteTimeoutMillis());
        this.config.getHttp().setCallTimeoutMillis(src.getCallTimeoutMillis());
        this.config.getHttp().setMaxIdleConnections(src.getMaxIdleConnections());
        this.config.getHttp().setKeepAliveDurationMillis(src.getKeepAliveDurationMillis());
        this.config.getHttp().setMaxRequests(src.getMaxRequests());
        this.config.getHttp().setMaxRequestsPerHost(src.getMaxRequestsPerHost());
        this.config.getHttp().setStreamCorePoolSize(src.getStreamCorePoolSize());
        this.config.getHttp().setStreamMaxPoolSize(src.getStreamMaxPoolSize());
        this.config.getHttp().setStreamQueueCapacity(src.getStreamQueueCapacity());
        this.config.getHttp().setStreamKeepAliveMillis(src.getStreamKeepAliveMillis());
        this.config.getHttp().setStreamEventQueueCapacity(src.getStreamEventQueueCapacity());
        this.config.getHttp().setRetryOnConnectionFailure(src.isRetryOnConnectionFailure());
        this.config.getHttp().setVerifySsl(src.isVerifySsl());
        this.config.getHttp().setDefaultModel(src.getDefaultModel());
        this.config.getHttp().setDefaultAgent(src.getDefaultAgent());
    }

    private void copyCliConfig(OpenCodeCliConfig src) {
        this.config.getCli().setEnabled(src.isEnabled());
        this.config.getCli().setStartupCheckEnabled(src.isStartupCheckEnabled());
        this.config.getCli().setFailFastOnUnavailable(src.isFailFastOnUnavailable());
        this.config.getCli().setExecutable(src.getExecutable());
        this.config.getCli().setTimeout(src.getTimeout());
        this.config.getCli().setProbeTimeoutSeconds(src.getProbeTimeoutSeconds());
        this.config.getCli().setWorkingDirectory(src.getWorkingDirectory());
        this.config.getCli().setMaxConcurrentExecutions(src.getMaxConcurrentExecutions());
    }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) {
        return httpClient.createSession(title);
    }

    public Session getSession(String sessionId) {
        return httpClient.getSession(sessionId);
    }

    public List<Session> listSessions() {
        return httpClient.listSessions();
    }

    public List<Session> listSessions(String search, Integer limit, Integer start) {
        return httpClient.listSessions(search, limit, start);
    }

    public java.util.Optional<Session> findSessionByTitle(String title) {
        return httpClient.findSessionByTitle(title);
    }

    public boolean deleteSession(String sessionId) {
        return httpClient.deleteSession(sessionId);
    }

    // ============================================================
    // Prompt
    // ============================================================

    public PromptResult chatCompletion(String sessionId, PromptRequest request) {
        return httpClient.prompt(sessionId, request);
    }

    public PromptResult chatCompletion(String sessionId, String text) {
        return httpClient.prompt(sessionId, PromptRequest.ofText(text));
    }

    public PromptResult chatCompletion(String sessionId, String text, String providerID, String modelID) {
        return httpClient.prompt(sessionId, PromptRequest.ofText(text, providerID, modelID));
    }

    // ----------------------------------------------------------------
    // sessionKey 模式
    // ----------------------------------------------------------------

    public PromptResult chatCompletionWithSession(PromptRequest request, String sessionKey) {
        return httpClient.chatCompletionWithSession(request, sessionKey);
    }

    public PromptResult chatCompletionWithSession(PromptRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        return httpClient.chatCompletionWithSession(request, sessionKey, cancellation);
    }

    public PromptResult chatCompletionWithSession(String text, String sessionKey) {
        return httpClient.chatCompletionWithSession(PromptRequest.ofText(text), sessionKey);
    }

    public PromptResult chatCompletionWithSession(String text, String providerID, String modelID, String sessionKey) {
        return httpClient.chatCompletionWithSession(PromptRequest.ofText(text, providerID, modelID), sessionKey);
    }

    public boolean chatCompletionWithSessionAsync(PromptRequest request, String sessionKey) {
        return httpClient.chatCompletionWithSessionAsync(request, sessionKey);
    }

    // ----------------------------------------------------------------
    // OpenAI 标准 ChatRequest/ChatResponse
    // ----------------------------------------------------------------

    public ChatResponse chatCompletion(String sessionId, ChatRequest request) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        PromptResult result = httpClient.prompt(sessionId, promptRequest);
        return ChatMessageMapper.toChatResponse(result);
    }

    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        PromptResult result = httpClient.chatCompletionWithSession(promptRequest, sessionKey);
        return ChatMessageMapper.toChatResponse(result);
    }

    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        PromptResult result = httpClient.chatCompletionWithSession(promptRequest, sessionKey, cancellation);
        return ChatMessageMapper.toChatResponse(result);
    }

    public ChatStreamingResponse chatCompletionStream(ChatRequest request, String sessionKey) {
        return chatCompletionStream(request, sessionKey, null);
    }

    public ChatStreamingResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context) {
        return chatCompletionStream(request, sessionKey, context, null);
    }

    /**
     * 流式对话，并在订阅启动前绑定增量回调，避免丢失首批分片。
     */
    public ChatStreamingResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context,
                                                       Consumer<String> deltaConsumer) {
        String sessionId = httpClient.ensureSession(sessionKey, context);
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);

        ChatStreamingResponse stream = new ChatStreamingResponse().onDelta(deltaConsumer);

        OpenCodeSseClient.QueueSubscription subscription =
                sseClient.subscribeQueueSubscription(context);
        java.util.concurrent.BlockingQueue<Event> queue = subscription.getQueue();

        try {
            streamExecutor.submit(() -> {
            try {
                long deadline = System.currentTimeMillis() + (config.getCli().getTimeout() * 1000L);
                while (!stream.isDone() && System.currentTimeMillis() < deadline) {
                    Event event = queue.poll(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (event == null) {
                        continue;
                    }

                    String eventSessionId = event.getProperties() != null
                            ? Objects.toString(event.getProperties().get("sessionID"), null) : null;
                    if (eventSessionId == null || !eventSessionId.equals(sessionId)) {
                        continue;
                    }

                    String type = event.getType();
                    if (type == null) {
                        continue;
                    }

                    if (type.contains("text.delta") || type.contains("message.part.updated")) {
                        String delta = extractDeltaText(event);
                        if (delta != null && !delta.isEmpty()) {
                            stream.acceptDelta(delta);
                        }
                    }

                    if (type.contains("session.status") || type.contains("session.idle")) {
                        String status = event.getProperties() != null
                                ? Objects.toString(event.getProperties().get("status"), null) : null;
                        if ("idle".equals(status) || type.contains("idle")) {
                            stream.finish();
                            return;
                        }
                    }

                    if (type.contains("session.error")) {
                        String error = event.getProperties() != null
                                ? Objects.toString(event.getProperties().get("error"), "unknown error") : "unknown error";
                        stream.fail(new RuntimeException(error));
                        return;
                    }
                }
                if (!stream.isDone()) {
                    stream.fail(new RuntimeException("Stream timed out for session: " + sessionId));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stream.fail(e);
            } catch (Exception e) {
                stream.fail(e);
            } finally {
                subscription.close();
            }
            });
        } catch (RejectedExecutionException error) {
            subscription.close();
            stream.fail(new IllegalStateException("OpenCode stream executor is full", error));
            return stream;
        }

        try {
            if (!httpClient.promptAsync(sessionId, promptRequest, context)) {
                subscription.close();
                stream.fail(new IllegalStateException("OpenCode async prompt was rejected"));
            }
        } catch (RuntimeException error) {
            subscription.close();
            stream.fail(error);
        }

        return stream;
    }

    private static String extractDeltaText(Event event) {
        if (event.getProperties() == null) {
            return null;
        }
        Object part = event.getProperties().get("part");
        if (part instanceof Map) {
            Object text = ((Map<?, ?>) part).get("text");
            if (text != null) {
                return text.toString();
            }
        }
        Object delta = event.getProperties().get("delta");
        if (delta != null) {
            return delta.toString();
        }
        return null;
    }

    public String ensureSession(String sessionKey) {
        return httpClient.ensureSession(sessionKey);
    }

    public boolean chatCompletionAsync(String sessionId, PromptRequest request) {
        return httpClient.promptAsync(sessionId, request);
    }

    public boolean chatCompletionAsync(String sessionId, String text) {
        return httpClient.promptAsync(sessionId, PromptRequest.ofText(text));
    }

    public List<PromptResult> getMessages(String sessionId) {
        return httpClient.getMessages(sessionId);
    }

    public boolean abort(String sessionId) {
        return httpClient.abortSession(sessionId);
    }

    // ============================================================
    // Agent
    // ============================================================

    public List<Agent> listAgents() {
        return httpClient.listAgents();
    }

    // ============================================================
    // Global
    // ============================================================

    public HealthStatus health() {
        return httpClient.health();
    }

    // ============================================================
    // SSE 事件流
    // ============================================================

    /** 获取统一的 OpenCode 聊天场景客户端。 */
    public OpenCodeChatClient chat() {
        return chatClient;
    }

    /** @deprecated 业务聊天请使用 {@link #chat()}，这里只保留原始事件订阅兼容入口。 */
    @Deprecated
    public OpenCodeSseClient sse() {
        return sseClient;
    }

    public OpenCodeSseClient eventStream() {
        return sseClient;
    }

    public okhttp3.sse.EventSource onSessionEvent(String sessionId,
            io.github.easy4j.opencode.api.event.EventHandler handler) {
        return sseClient.subscribeHandler(sessionId, handler);
    }

    public okhttp3.sse.EventSource onEvent(
            io.github.easy4j.opencode.api.event.EventHandler handler) {
        return sseClient.subscribeHandler(null, handler);
    }

    public okhttp3.sse.EventSource onEventTypes(java.util.Set<String> types,
            java.util.function.Consumer<Event> consumer) {
        return sseClient.subscribeEventTypes(types, consumer);
    }

    // ============================================================
    // CLI
    // ============================================================

    public OpenCodeCli cli() {
        return cli;
    }

    // ============================================================
    // Config
    // ============================================================

    public OpenCodeClientConfig getConfig() {
        return config;
    }

    public OpenCodeConfig getOpenCodeConfig() {
        return httpClient.getConfig();
    }

    public OpenCodeConfig getGlobalOpenCodeConfig() {
        return httpClient.getGlobalConfig();
    }

    public OpenCodeConfig updateOpenCodeConfig(Object body) {
        return httpClient.updateConfig(body);
    }

    public OpenCodeConfig updateGlobalOpenCodeConfig(Object body) {
        return httpClient.updateGlobalConfig(body);
    }

    public ProviderList getConfigProviders() {
        return httpClient.getConfigProviders();
    }

    // ============================================================
    // Project
    // ============================================================

    public List<Project> listProjects() {
        return httpClient.listProjects();
    }

    public Project getCurrentProject() {
        return httpClient.getCurrentProject();
    }

    public Project updateProject(String projectId, Object body) {
        return httpClient.updateProject(projectId, body);
    }

    public boolean initProjectGit() {
        return httpClient.initProjectGit();
    }

    // ============================================================
    // Provider
    // ============================================================

    public ProviderList listProviders() {
        return httpClient.listProviders();
    }

    public Map<String, List<ProviderAuthMethod>> listProviderAuthMethods() {
        return httpClient.listProviderAuthMethods();
    }

    public ProviderAuthAuthorization providerOAuthAuthorize(String providerId, String method) {
        return httpClient.providerOAuthAuthorize(providerId, method);
    }

    public boolean providerOAuthCallback(String providerId, String code) {
        return httpClient.providerOAuthCallback(providerId, code);
    }

    // ============================================================
    // File / Find
    // ============================================================

    public List<FileNode> listFiles(String path) {
        return httpClient.listFiles(path);
    }

    public FileContent getFileContent(String path) {
        return httpClient.getFileContent(path);
    }

    public List<FileNode> getFileStatus() {
        return httpClient.getFileStatus();
    }

    public List<FileSearchResult> find(String pattern) {
        return httpClient.find(pattern);
    }

    public List<String> findFiles(String query) {
        return httpClient.findFiles(query);
    }

    public List<Symbol> findSymbols(String query) {
        return httpClient.findSymbols(query);
    }

    // ============================================================
    // Misc
    // ============================================================

    public List<Command> listCommands() {
        return httpClient.listCommands();
    }

    public List<Skill> listSkills() {
        return httpClient.listSkills();
    }

    public List<FormatterStatus> listFormatters() {
        return httpClient.listFormatters();
    }

    public List<LspStatus> listLsps() {
        return httpClient.listLsps();
    }

    public Map<String, McpStatus> listMcpServers() {
        return httpClient.listMcpServers();
    }

    public McpStatus addMcpServer(String name, Object config) {
        return httpClient.addMcpServer(name, config);
    }

    public OpenCodePath getPath() {
        return httpClient.getPath();
    }

    public VcsInfo getVcs() {
        return httpClient.getVcs();
    }

    // ============================================================
    // Session extended
    // ============================================================

    public Map<String, SessionStatus> getSessionStatusMap() {
        return httpClient.getSessionStatusMap();
    }

    public List<Session> getSessionChildren(String sessionId) {
        return httpClient.getSessionChildren(sessionId);
    }

    public List<SessionTodo> getSessionTodo(String sessionId) {
        return httpClient.getSessionTodo(sessionId);
    }

    public List<FileDiff> getSessionDiff(String sessionId, String messageId) {
        return httpClient.getSessionDiff(sessionId, messageId);
    }

    public Session shareSession(String sessionId) {
        return httpClient.shareSession(sessionId);
    }

    public Session unshareSession(String sessionId) {
        return httpClient.unshareSession(sessionId);
    }

    public Session forkSession(String sessionId, String messageId) {
        return httpClient.forkSession(sessionId, messageId);
    }

    public boolean initSession(String sessionId, String messageId, String providerId, String modelId) {
        return httpClient.initSession(sessionId, messageId, providerId, modelId);
    }

    public boolean summarizeSession(String sessionId, String providerId, String modelId) {
        return httpClient.summarizeSession(sessionId, providerId, modelId);
    }

    public boolean revertSession(String sessionId, String messageId, String partId) {
        return httpClient.revertSession(sessionId, messageId, partId);
    }

    public boolean unrevertSession(String sessionId) {
        return httpClient.unrevertSession(sessionId);
    }

    public MessageInfo getMessage(String sessionId, String messageId) {
        return httpClient.getMessage(sessionId, messageId);
    }

    public PromptResult runSessionCommand(String sessionId, String command, String arguments,
                                          String agent, String model) {
        return httpClient.runSessionCommand(sessionId, command, arguments, agent, model);
    }

    // ============================================================
    // Question
    // ============================================================

    public List<QuestionRequest> listQuestions() {
        return httpClient.listQuestions();
    }

    public boolean replyQuestion(String requestId, List<String> answers) {
        return httpClient.replyQuestion(requestId, answers);
    }

    public boolean rejectQuestion(String requestId) {
        return httpClient.rejectQuestion(requestId);
    }

    // ============================================================
    // Permission
    // ============================================================

    public List<PermissionRequest> listPermissions() {
        return httpClient.listPermissions();
    }

    public boolean replyPermission(String requestId, String response, boolean remember) {
        return httpClient.replyPermission(requestId, response, remember);
    }

    // ============================================================
    // Auth
    // ============================================================

    public boolean setAuth(String providerId, Object body) {
        return httpClient.setAuth(providerId, body);
    }

    public boolean removeAuth(String providerId) {
        return httpClient.removeAuth(providerId);
    }

    // ============================================================
    // Instance / Global lifecycle
    // ============================================================

    public boolean disposeInstance() {
        return httpClient.disposeInstance();
    }

    public boolean globalDispose() {
        return httpClient.globalDispose();
    }

    public boolean globalUpgrade(String target) {
        return httpClient.globalUpgrade(target);
    }

    // ============================================================
    // CLI facade
    // ============================================================

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliServe() {
        return cli.serve(null, null);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliServe(Integer port, String hostname) {
        return cli.serve(port, hostname);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliWeb() {
        return cli.web(null, null);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliWeb(Integer port, String hostname) {
        return cli.web(port, hostname);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAcp(String cwd) {
        return cli.acp(cwd);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliGenerate() {
        return cli.generate();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAttach(String url, String dir,
                                                                     String sessionId,
                                                                     String username, String password) {
        return cli.attach(url, dir, sessionId, username, password);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliUpgrade() {
        return cli.upgrade();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliUpgrade(String target, String method) {
        return cli.upgrade(target, method);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliUninstall(boolean keepConfig,
                                                                         boolean keepData,
                                                                         boolean dryRun,
                                                                         boolean force) {
        return cli.uninstall(keepConfig, keepData, dryRun, force);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliStats() {
        return cli.stats(null, null, null, null);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliStats(Integer days, Integer tools,
                                                                    Integer models, String project) {
        return cli.stats(days, tools, models, project);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliExport(String sessionId, boolean sanitize) {
        return cli.export(sessionId, sanitize);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliImport(String fileOrUrl) {
        return cli.importSession(fileOrUrl);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliSessionList() {
        return cli.sessionList();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliSessionList(int maxCount) {
        return cli.sessionList(maxCount);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAgentList() {
        return cli.agentList();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAgentCreate(String path,
                                                                           String description,
                                                                           String mode,
                                                                           String permissions,
                                                                           String model) {
        return cli.agentCreate(path, description, mode, permissions, model);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliModels() {
        return cli.models();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliModels(String provider, boolean verbose,
                                                                    boolean refresh) {
        return cli.models(provider, verbose, refresh);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliProvidersList() {
        return cli.providersList();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliProvidersLogin(String provider,
                                                                             String method) {
        return cli.providersLogin(provider, method);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliProvidersLogout(String provider) {
        return cli.providersLogout(provider);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAuthList() {
        return cli.authList();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAuthLogin(String provider, String method) {
        return cli.authLogin(provider, method);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAuthLogout(String provider) {
        return cli.authLogout(provider);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliMcpList() {
        return cli.mcpList();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliMcpAdd(String name, String url) {
        return cli.mcpAdd(name, url);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliMcpLogout(String name) {
        return cli.mcpLogout(name);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliMcpAuth(String name) {
        return cli.mcpAuth(name);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDb(String query, String format) {
        return cli.db(query, format);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDbPath() {
        return cli.dbPath();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDebugConfig() {
        return cli.debugConfig();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDebugPaths() {
        return cli.debugPaths();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDebugInfo() {
        return cli.debugInfo();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliGithubInstall() {
        return cli.githubInstall();
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliGithubRun(String event, String token) {
        return cli.githubRun(event, token);
    }

    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliPr(int number) {
        return cli.pr(number);
    }

    // ============================================================
    // 生命周期
    // ============================================================

    @Override
    public void close() {
        if (streamExecutor != null) streamExecutor.shutdownNow();
        if (httpClient != null) httpClient.close();
        if (sseClient != null) sseClient.close();
    }
}
