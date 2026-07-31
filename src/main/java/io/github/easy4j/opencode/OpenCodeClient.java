package io.github.easy4j.opencode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.api.mapper.ChatMessageMapper;
import io.github.easy4j.opencode.api.model.*;
import io.github.easy4j.opencode.cli.OpenCodeCli;
import io.github.easy4j.opencode.cli.OpenCodeCliExecutor;
import io.github.easy4j.opencode.api.OpenCodeHttpClient;
import io.github.easy4j.opencode.api.OpenCodeRequestContext;
import io.github.easy4j.opencode.api.OpenCodeSseClient;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenCode 客户端门面：HTTP Server + SSE 事件流 + 本地 CLI。
 * <p>
 * 三条通信通道相互独立：
 * </p>
 * <ul>
 *     <li><b>HTTP</b>：{@link #chatCompletion} / {@link #chatCompletionAsync} / {@link #createSession} 等 — REST API</li>
 *     <li><b>SSE</b>：{@link #sse()} — 事件流</li>
 *     <li><b>CLI</b>：{@link #cli()} — 本地 {@code opencode} 命令封装</li>
 * </ul>
 */
public class OpenCodeClient implements AutoCloseable {

    private final OpenCodeClientConfig config;
    private final OpenCodeHttpClient httpClient;
    private final OpenCodeSseClient sseClient;
    private final OpenCodeCli cli;

    /**
     * 使用 HTTP 与 CLI 独立配置构造客户端。
     */
    public OpenCodeClient(OpenCodeHttpClientConfig httpConfig, OpenCodeCliConfig cliConfig) {
        this(httpConfig, cliConfig, null, null);
    }

    /**
     * 使用 HTTP 与 CLI 独立配置构造客户端，可注入共享 OkHttp 与 ObjectMapper。
     */
    public OpenCodeClient(OpenCodeHttpClientConfig httpConfig, OpenCodeCliConfig cliConfig,
                          ObjectMapper objectMapper, OkHttpClient httpClient) {
        Objects.requireNonNull(httpConfig, "httpConfig");
        Objects.requireNonNull(cliConfig, "cliConfig");
        this.config = new OpenCodeClientConfig();
        this.config.getHttp().setServerUrl(httpConfig.getServerUrl());
        this.config.getHttp().setUsername(httpConfig.getUsername());
        this.config.getHttp().setPassword(httpConfig.getPassword());
        this.config.getHttp().setConnectTimeoutMillis(httpConfig.getConnectTimeoutMillis());
        this.config.getHttp().setReadTimeoutMillis(httpConfig.getReadTimeoutMillis());
        this.config.getHttp().setVerifySsl(httpConfig.isVerifySsl());
        this.config.getHttp().setDefaultModel(httpConfig.getDefaultModel());
        this.config.getHttp().setDefaultAgent(httpConfig.getDefaultAgent());
        this.config.getCli().setExecutable(cliConfig.getExecutable());
        this.config.getCli().setTimeout(cliConfig.getTimeout());
        this.config.getCli().setProbeTimeoutSeconds(cliConfig.getProbeTimeoutSeconds());
        this.config.getCli().setWorkingDirectory(cliConfig.getWorkingDirectory());
        this.config.getCli().setMaxConcurrentExecutions(cliConfig.getMaxConcurrentExecutions());
        this.httpClient = new OpenCodeHttpClient(httpConfig, objectMapper, httpClient);
        this.sseClient = new OpenCodeSseClient(httpConfig, objectMapper,
                httpClient != null ? httpClient : this.httpClient.getOkHttpClient());
        this.cli = new OpenCodeCli(new OpenCodeCliExecutor(cliConfig));
    }

    /**
     * 使用组合配置构造客户端。
     */
    public OpenCodeClient(OpenCodeClientConfig config) {
        this(config, null, null);
    }

    /**
     * 标准构造（自动创建 HTTP、SSE、CLI 客户端）。
     */
    public OpenCodeClient(OpenCodeClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(Objects.requireNonNull(config, "config").getHttp(),
                config.getCli(),
                objectMapper,
                httpClient);
    }

    /**
     * 完整依赖注入（用于测试或自定义组件）。
     */
    public OpenCodeClient(OpenCodeClientConfig config,
                          OpenCodeHttpClient httpClient,
                          OpenCodeSseClient sseClient,
                          OpenCodeCli cli) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.sseClient = Objects.requireNonNull(sseClient, "sseClient");
        this.cli = Objects.requireNonNull(cli, "cli");
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

    /** 分页/过滤列出 sessions（服务端 search/limit/start 过滤）。 */
    public List<Session> listSessions(String search, Integer limit, Integer start) {
        return httpClient.listSessions(search, limit, start);
    }

    /** 按 title 精确查找 session，未命中返回 {@link java.util.Optional#empty()}。 */
    public java.util.Optional<Session> findSessionByTitle(String title) {
        return httpClient.findSessionByTitle(title);
    }

    public boolean deleteSession(String sessionId) {
        return httpClient.deleteSession(sessionId);
    }

    // ============================================================
    // Prompt
    // ============================================================

    /**
     * 发送 prompt 并同步等待 AI 响应。
     *
     * @param sessionId 会话 ID
     * @param request   prompt 请求
     * @return AI 响应
     */
    public PromptResult chatCompletion(String sessionId, PromptRequest request) {
        return httpClient.prompt(sessionId, request);
    }

    /**
     * 快捷方式：发送纯文本 prompt。
     */
    public PromptResult chatCompletion(String sessionId, String text) {
        return httpClient.prompt(sessionId, PromptRequest.ofText(text));
    }

    /**
     * 快捷方式：发送纯文本 prompt 并指定模型。
     */
    public PromptResult chatCompletion(String sessionId, String text, String providerID, String modelID) {
        return httpClient.prompt(sessionId, PromptRequest.ofText(text, providerID, modelID));
    }

    // ----------------------------------------------------------------
    // sessionKey 模式（对齐 Hermes/OpenClaw chatCompletionWithSession）
    // ----------------------------------------------------------------

    /**
     * 按 sessionKey 发送消息并同步等待 AI 响应。
     * <p>底层基于会话模型（ensureSession + prompt），对外封装为与 Hermes/OpenClaw 对称的
     * {@code chatCompletionWithSession} 接口。sessionKey 同时作为 session 的 title，
     * {@code ensureSession} 保证 session 存在（不存在则创建）。
     * 建议用 {@link io.github.easy4j.opencode.api.OpenCodeSessionKeys} 生成 sessionKey。</p>
     *
     * @param request    prompt 请求
     * @param sessionKey 会话复用 key
     * @return AI 响应
     */
    public PromptResult chatCompletionWithSession(PromptRequest request, String sessionKey) {
        return httpClient.chatCompletionWithSession(request, sessionKey);
    }

    /**
     * 按 sessionKey 发送纯文本消息并同步等待 AI 响应。
     */
    public PromptResult chatCompletionWithSession(String text, String sessionKey) {
        return httpClient.chatCompletionWithSession(PromptRequest.ofText(text), sessionKey);
    }

    /**
     * 按 sessionKey 发送纯文本消息并指定模型。
     */
    public PromptResult chatCompletionWithSession(String text, String providerID, String modelID, String sessionKey) {
        return httpClient.chatCompletionWithSession(PromptRequest.ofText(text, providerID, modelID), sessionKey);
    }

    /**
     * 按 sessionKey 异步发送消息，不等待响应。
     */
    public boolean chatCompletionWithSessionAsync(PromptRequest request, String sessionKey) {
        return httpClient.chatCompletionWithSessionAsync(request, sessionKey);
    }

    // ----------------------------------------------------------------
    // OpenAI 标准 ChatRequest/ChatResponse（对齐 OpenClaw/Hermes）
    // ----------------------------------------------------------------

    /**
     * 按 sessionId 发送 OpenAI 标准请求并同步等待响应。
     * <p>内部自动将 {@link ChatRequest} 转换为 {@link PromptRequest}，
     * 将 {@link PromptResult} 转换为 {@link ChatResponse}。</p>
     *
     * @param sessionId 会话 ID
     * @param request   OpenAI 标准请求
     * @return OpenAI 标准响应
     */
    public ChatResponse chatCompletion(String sessionId, ChatRequest request) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        PromptResult result = httpClient.prompt(sessionId, promptRequest);
        return ChatMessageMapper.toChatResponse(result);
    }

    /**
     * 按 sessionKey 发送 OpenAI 标准请求并同步等待响应。
     * <p>与 OpenClaw/Hermes 的 {@code chatCompletionWithSession(request, sessionKey)} 完全对称。</p>
     *
     * @param request    OpenAI 标准请求
     * @param sessionKey 会话复用 key
     * @return OpenAI 标准响应
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        PromptResult result = httpClient.chatCompletionWithSession(promptRequest, sessionKey);
        return ChatMessageMapper.toChatResponse(result);
    }

    /**
     * 按 sessionKey 流式发送消息，返回 {@link ChatStreamingResponse}。
     * <p>
     * 内部流程：ensureSession → promptAsync（不阻塞）→ 订阅全局 SSE 事件流 →
     * 按 sessionId 过滤事件，累积 text delta → session idle 时完成 future。
     * </p>
     * <p>与 Hermes 的 {@code chatCompletionStream} 对称，调用方可通过
     * {@link ChatStreamingResponse#onDelta(Consumer)} 注册增量回调，
     * 或通过 {@link ChatStreamingResponse#get()} 阻塞等待完整文本。</p>
     *
     * @param request    OpenAI 标准请求
     * @param sessionKey 会话复用 key
     * @return 流式响应（CompletableFuture，完成时携带完整文本）
     */
    public ChatStreamingResponse chatCompletionStream(ChatRequest request, String sessionKey) {
        return chatCompletionStream(request, sessionKey, null);
    }

    /**
     * 在指定 OpenCode 工作目录中按 sessionKey 流式发送消息。
     *
     * @param request 请求
     * @param sessionKey 稳定会话键
     * @param context 包含受控工作目录的请求上下文
     * @return 流式响应
     */
    public ChatStreamingResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context) {
        String sessionId = httpClient.ensureSession(sessionKey, context);
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);

        ChatStreamingResponse stream = new ChatStreamingResponse();

        // 订阅全局 SSE，按 sessionId 过滤事件
        java.util.concurrent.BlockingQueue<Event> queue = sseClient.subscribeQueue(context);

        // 异步消费事件
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                long deadline = System.currentTimeMillis() + (config.getCli().getTimeout() * 1000L);
                while (!stream.isDone() && System.currentTimeMillis() < deadline) {
                    Event event = queue.poll(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (event == null) {
                        continue;
                    }

                    // 按 sessionId 过滤
                    String eventSessionId = event.getProperties() != null
                            ? Objects.toString(event.getProperties().get("sessionID"), null) : null;
                    if (eventSessionId == null || !eventSessionId.equals(sessionId)) {
                        continue;
                    }

                    String type = event.getType();
                    if (type == null) {
                        continue;
                    }

                    // text delta 事件
                    if (type.contains("text.delta") || type.contains("message.part.updated")) {
                        String delta = extractDeltaText(event);
                        if (delta != null && !delta.isEmpty()) {
                            stream.acceptDelta(delta);
                        }
                    }

                    // session idle = 完成
                    if (type.contains("session.status") || type.contains("session.idle")) {
                        String status = event.getProperties() != null
                                ? Objects.toString(event.getProperties().get("status"), null) : null;
                        if ("idle".equals(status) || type.contains("idle")) {
                            stream.finish();
                            return;
                        }
                    }

                    // session error
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
            }
        });

        // 触发异步 prompt（不阻塞，SSE 事件驱动结果）
        httpClient.promptAsync(sessionId, promptRequest, context);

        return stream;
    }

    /**
     * 从事件属性中提取增量文本。
     */
    private static String extractDeltaText(Event event) {
        if (event.getProperties() == null) {
            return null;
        }
        // 尝试 part.text
        Object part = event.getProperties().get("part");
        if (part instanceof Map) {
            Object text = ((Map<?, ?>) part).get("text");
            if (text != null) {
                return text.toString();
            }
        }
        // 尝试 delta
        Object delta = event.getProperties().get("delta");
        if (delta != null) {
            return delta.toString();
        }
        return null;
    }

    /**
     * 确保 sessionKey 对应的 session 存在，返回其 sessionId。
     */
    public String ensureSession(String sessionKey) {
        return httpClient.ensureSession(sessionKey);
    }

    /**
     * 异步发送消息，不等待响应。
     *
     * @param sessionId 会话 ID
     * @param request   prompt 请求
     * @return 是否成功提交
     */
    public boolean chatCompletionAsync(String sessionId, PromptRequest request) {
        return httpClient.promptAsync(sessionId, request);
    }

    /**
     * 快捷方式：异步发送纯文本消息。
     */
    public boolean chatCompletionAsync(String sessionId, String text) {
        return httpClient.promptAsync(sessionId, PromptRequest.ofText(text));
    }

    public List<PromptResult> getMessages(String sessionId) {
        return httpClient.getMessages(sessionId);
    }

    /**
     * 中止正在运行的会话。
     */
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

    /**
     * 获取 SSE 客户端实例。
     */
    public OpenCodeSseClient sse() {
        return sseClient;
    }

    /**
     * {@code sse()} 的别名，等价。
     */
    public OpenCodeSseClient eventStream() {
        return sseClient;
    }

    /**
     * 订阅指定 session 的事件，使用类型化 {@link io.github.easy4j.opencode.api.event.EventHandler}。
     *
     * @return EventSource，可调用 cancel() 停止订阅
     */
    public okhttp3.sse.EventSource onSessionEvent(String sessionId,
            io.github.easy4j.opencode.api.event.EventHandler handler) {
        return sseClient.subscribeHandler(sessionId, handler);
    }

    /**
     * 订阅全局事件流（不按 session 过滤），使用类型化 handler。
     */
    public okhttp3.sse.EventSource onEvent(
            io.github.easy4j.opencode.api.event.EventHandler handler) {
        return sseClient.subscribeHandler(null, handler);
    }

    /**
     * 订阅特定事件类型集合。
     *
     * @param types 事件类型白名单
     */
    public okhttp3.sse.EventSource onEventTypes(java.util.Set<String> types,
            java.util.function.Consumer<Event> consumer) {
        return sseClient.subscribeEventTypes(types, consumer);
    }

    // ============================================================
    // CLI
    // ============================================================

    /**
     * 本地 CLI 命令封装。
     */
    public OpenCodeCli cli() {
        return cli;
    }

    // ============================================================
    // Config
    // ============================================================

    public OpenCodeClientConfig getConfig() {
        return config;
    }

    // ============================================================
    // Config（HTTP 层，独立于上面的 getConfig() OpenCodeClientConfig）
    // ============================================================

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
    // Misc (Command / Skill / Formatter / Lsp / MCP / Path / VCS)
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
    // CLI facade（cliServe、cliWeb 等是新增的便捷别名，与 cli().serve() 等价）
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
        httpClient.close();
        sseClient.close();
    }
}
