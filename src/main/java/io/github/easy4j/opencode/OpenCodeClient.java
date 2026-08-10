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
import io.github.easy4j.opencode.api.sse.StreamingChatResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Facade client for OpenCode: HTTP Server + SSE event stream + local CLI.
 * <p>Three communication channels are independent of each other and are created based on
 * the {@code enabled} flag in their respective sub-configurations:</p>
 * <ul>
 *     <li>{@code OpenCodeHttpClientConfig.enabled = false} &rarr; HTTP/SSE 子客户端不可用</li>
 *     <li>{@code OpenCodeCliConfig.enabled = false} &rarr; CLI 子客户端不可用</li>
 * </ul>
 * <h2>Constructor Selection</h2>
 * <p>Eight overloads cover three scenarios:</p>
 * <ul>
 *     <li>HTTP only / CLI only: pass a single sub-config; the other subsystem is disabled</li>
 *     <li>HTTP + CLI: pass two sub-configs; each subsystem is enabled per its own {@code enabled} flag</li>
 *     <li>Combined config: pass {@link OpenCodeClientConfig}; internally split into two sub-configs</li>
 * </ul>
 * <p>Each scenario has a variant with auto-created ObjectMapper/OkHttpClient and a variant with
 * forced injection (the injected version applies {@code requireNonNull} validation).</p>
 * <h3>Startup Health Checks</h3>
 * <p>After subsystem initialization, the primary constructor runs health probes based on
 * {@code startupCheckEnabled} and {@code failFastOnUnavailable} (HTTP: {@code GET /global/health};
 * CLI: {@code opencode --version}). If the probe fails but fail-fast is not enabled, only a
 * WARN log is emitted and construction continues.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeHttpClientConfig
 * @see OpenCodeCliConfig
 * @see OpenCodeClientConfig
 */
@Slf4j
public class OpenCodeClient implements AutoCloseable {

    /**
     * 当前客户端使用的不可变配置引用。
     */
    private final OpenCodeClientConfig config;
    /**
     * 执行连接复用和异步网络请求的 OkHttp 客户端。
     */
    private final OpenCodeHttpClient httpClient;
    /**
     * 当前组件复用的聊天客户端；资源所有权由对应 owns 字段决定。
     */
    private final OpenCodeChatClient chatClient;
    /**
     * 当前组件复用的SSE 客户端；资源所有权由对应 owns 字段决定。
     */
    private final OpenCodeSseClient sseClient;
    /**
     * 本地 CLI 操作门面；CLI 子系统禁用时为 {@code null}。
     */
    private final OpenCodeCli cli;
    /**
     * 由当前门面创建并负责关闭的 OkHttp 客户端；使用外部客户端时为 {@code null}。
     */
    private final OkHttpClient ownedHttpClient;

    // ============================================================
    // 构造器
    // ============================================================

    /**
     * 仅 HTTP 子系统（CLI 禁用）。自动创建默认 ObjectMapper 与 OkHttpClient。
     *
     * @param httpConfig HTTP 子系统配置；为 {@code null} 时禁用 HTTP 能力
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
     */
    public OpenCodeClient(OpenCodeHttpClientConfig httpConfig) {
        this(httpConfig, new OpenCodeCliConfig(), new ObjectMapper(), null);
    }

    /**
     * 仅 HTTP 子系统（CLI 禁用），强制注入共享 ObjectMapper 与 OkHttpClient。
     *
     * @param httpConfig HTTP 子系统配置；为 {@code null} 时禁用 HTTP 能力
     * @param objectMapper JSON 映射器；为 {@code null} 时使用 SDK 默认配置
     * @param httpClient 可复用的 OkHttp 客户端；为 {@code null} 时由 SDK 创建
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
     */
    public OpenCodeClient(OpenCodeHttpClientConfig httpConfig, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(httpConfig, new OpenCodeCliConfig(), objectMapper, httpClient);
    }

    /**
     * 仅 CLI 子系统（HTTP 禁用）。自动创建默认 ObjectMapper 与 OkHttpClient。
     *
     * @param cliConfig CLI 子系统配置；为 {@code null} 时禁用 CLI 能力
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
     */
    public OpenCodeClient(OpenCodeCliConfig cliConfig) {
        this(new OpenCodeHttpClientConfig(), cliConfig, new ObjectMapper(), null);
    }

    /**
     * 仅 CLI 子系统（HTTP 禁用），强制注入共享 ObjectMapper 与 OkHttpClient。
     *
     * @param cliConfig CLI 子系统配置；为 {@code null} 时禁用 CLI 能力
     * @param objectMapper JSON 映射器；为 {@code null} 时使用 SDK 默认配置
     * @param httpClient 可复用的 OkHttp 客户端；为 {@code null} 时由 SDK 创建
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
     */
    public OpenCodeClient(OpenCodeCliConfig cliConfig, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(new OpenCodeHttpClientConfig(), cliConfig, objectMapper, httpClient);
    }

    /**
     * HTTP + CLI 子系统。自动创建默认 ObjectMapper 与 OkHttpClient。
     *
     * @param httpConfig HTTP 子系统配置；为 {@code null} 时禁用 HTTP 能力
     * @param cliConfig CLI 子系统配置；为 {@code null} 时禁用 CLI 能力
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
     */
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
     *
     * @param httpConfig HTTP 子系统配置；为 {@code null} 时禁用 HTTP 能力
     * @param cliConfig CLI 子系统配置；为 {@code null} 时禁用 CLI 能力
     * @param objectMapper JSON 映射器；为 {@code null} 时使用 SDK 默认配置
     * @param httpClient 可复用的 OkHttp 客户端；为 {@code null} 时由 SDK 创建
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
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

        // HTTP、Chat 和 SSE 共享同一 OkHttp 连接池与 Dispatcher；仅 SDK 自建客户端由门面关闭。
        if (httpEnabled) {
            OkHttpClient sharedHttpClient = Objects.nonNull(httpClient)
                    ? httpClient : OpenCodeOkHttpClientFactory.create(httpConfig);
            this.ownedHttpClient = Objects.isNull(httpClient) ? sharedHttpClient : null;
            this.sseClient = new OpenCodeSseClient(
                    httpConfig, objectMapper, sharedHttpClient);
            this.chatClient = new OpenCodeChatClient(
                    httpConfig, objectMapper, sharedHttpClient, sseClient);
            this.httpClient = this.chatClient;
        } else {
            this.httpClient = null;
            this.chatClient = null;
            this.sseClient = null;
            this.ownedHttpClient = null;
        }

        // CLI 是独立子系统，关闭或禁用 HTTP 不影响本地 CLI 命令能力。
        if (cliEnabled) {
            this.cli = new OpenCodeCli(new OpenCodeCliExecutor(cliConfig));
        } else {
            this.cli = null;
        }

        // 启动自检仅在配置显式开启时执行；failFast 决定探测失败是否中止构造。
        runStartupChecks(httpConfig, cliConfig);
    }

    /**
     * 组合配置，自动创建默认 ObjectMapper 与 OkHttpClient。
     *
     * @param config 客户端配置；不得为 {@code null}
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
     */
    public OpenCodeClient(OpenCodeClientConfig config) {
        this(config, new ObjectMapper(), null);
    }

    /**
     * 组合配置，强制注入共享 ObjectMapper 与 OkHttpClient。
     *
     * @param config 客户端配置；不得为 {@code null}
     * @param objectMapper JSON 映射器；为 {@code null} 时使用 SDK 默认配置
     * @param httpClient 可复用的 OkHttp 客户端；为 {@code null} 时由 SDK 创建
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
     */
    public OpenCodeClient(OpenCodeClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(Objects.requireNonNull(config, "config").getHttp(),
                config.getCli(),
                objectMapper,
                httpClient);
    }

    /**
     * 全量依赖注入（用于测试或自定义组件）。
     * <p>使用此构造方法<b>不会</b>执行任何启动自检。</p>
     *
     * @param config 客户端配置；不得为 {@code null}
     * @param httpClient 可复用的 OkHttp 客户端；为 {@code null} 时由 SDK 创建
     * @param sseClient SSE 客户端；为 {@code null} 时按配置创建
     * @param cli CLI 门面；为 {@code null} 时按配置创建
     * @throws IllegalStateException 启用启动检查和快速失败后，对应子系统不可用
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
        this.ownedHttpClient = null;
    }

    // ============================================================
    // 启动自检
    // ============================================================

    private void runStartupChecks(OpenCodeHttpClientConfig httpConfig, OpenCodeCliConfig cliConfig) {
        if (httpConfig.isEnabled() && httpConfig.isStartupCheckEnabled()) {
            try {
                httpClient.health();
                log.info("OpenCode HTTP health check passed: {}", httpConfig.getBaseUrl());
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

    /**
     * HTTP 子系统是否启用。
     *
     * @return 满足条件返回 {@code true}，否则返回 {@code false}
     */
    public boolean isHttpEnabled() {
        return httpClient != null;
    }

    /**
     * CLI 子系统是否启用。
     *
     * @return 满足条件返回 {@code true}，否则返回 {@code false}
     */
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
        this.config.getHttp().setBaseUrl(src.getBaseUrl());
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

    /**
     * 通过 OpenCode Server HTTP API 创建会话。
     *
     * @param title 会话标题；用于展示和稳定会话查找
     * @return OpenCode SDK 返回的会话对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Session createSession(String title) {
        return httpClient.createSession(title);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取会话。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return OpenCode SDK 返回的会话对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Session getSession(String sessionId) {
        return httpClient.getSession(sessionId);
    }

    /**
     * 通过 OpenCode Server HTTP API 查询会话；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的会话列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<Session> listSessions() {
        return httpClient.listSessions();
    }

    /**
     * 通过 OpenCode Server HTTP API 查询会话；无数据时返回空集合。
     *
     * @param search 标题搜索条件；为空时不限制
     * @param limit 最大返回数量；为 {@code null} 时使用服务端默认值
     * @param start 分页起始偏移；为 {@code null} 时从首项开始
     * @return OpenCode Server 返回的会话列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<Session> listSessions(String search, Integer limit, Integer start) {
        return httpClient.listSessions(search, limit, start);
    }

    /**
     * 调用 OpenCode Server 搜索接口查找标题匹配的会话；未命中时返回空结果。
     *
     * @param title 会话标题；用于展示和稳定会话查找
     * @return 匹配的 OpenCode 会话；未找到时为 {@link java.util.Optional#empty()}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public java.util.Optional<Session> findSessionByTitle(String title) {
        return httpClient.findSessionByTitle(title);
    }

    /**
     * 请求 OpenCode Server 删除会话。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean deleteSession(String sessionId) {
        return httpClient.deleteSession(sessionId);
    }

    // ============================================================
    // Prompt
    // ============================================================

    /**
     * 同步提交聊天请求并返回完整响应。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param request 请求对象；不得为 {@code null}
     * @return OpenCode SDK 返回的Prompt 结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public PromptResult chatCompletion(String sessionId, PromptRequest request) {
        return httpClient.prompt(sessionId, request);
    }

    /**
     * 同步提交聊天请求并返回完整响应。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param text 发送给模型的文本内容
     * @return OpenCode SDK 返回的Prompt 结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public PromptResult chatCompletion(String sessionId, String text) {
        return httpClient.prompt(sessionId, PromptRequest.ofText(text));
    }

    /**
     * 同步提交聊天请求并返回完整响应。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param text 发送给模型的文本内容
     * @param providerID 模型提供方 ID
     * @param modelID 模型 ID
     * @return OpenCode SDK 返回的Prompt 结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public PromptResult chatCompletion(String sessionId, String text, String providerID, String modelID) {
        return httpClient.prompt(sessionId, PromptRequest.ofText(text, providerID, modelID));
    }

    // ----------------------------------------------------------------
    // sessionKey 模式
    // ----------------------------------------------------------------

    /**
     * 复用或创建稳定会话后提交聊天请求并返回结果。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return OpenCode SDK 返回的Prompt 结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public PromptResult chatCompletionWithSession(PromptRequest request, String sessionKey) {
        return httpClient.chatCompletionWithSession(request, sessionKey);
    }

    /**
     * 复用或创建稳定会话后提交聊天请求并返回结果。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param cancellation 取消信号；为 {@code null} 时不可由外部取消
     * @return OpenCode SDK 返回的Prompt 结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public PromptResult chatCompletionWithSession(PromptRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        return httpClient.chatCompletionWithSession(request, sessionKey, cancellation);
    }

    /**
     * 复用或创建稳定会话后提交聊天请求并返回结果。
     *
     * @param text 发送给模型的文本内容
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return OpenCode SDK 返回的Prompt 结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public PromptResult chatCompletionWithSession(String text, String sessionKey) {
        return httpClient.chatCompletionWithSession(PromptRequest.ofText(text), sessionKey);
    }

    /**
     * 复用或创建稳定会话后提交聊天请求并返回结果。
     *
     * @param text 发送给模型的文本内容
     * @param providerID 模型提供方 ID
     * @param modelID 模型 ID
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return OpenCode SDK 返回的Prompt 结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public PromptResult chatCompletionWithSession(String text, String providerID, String modelID, String sessionKey) {
        return httpClient.chatCompletionWithSession(PromptRequest.ofText(text, providerID, modelID), sessionKey);
    }

    /**
     * 异步复用或创建稳定会话后提交聊天请求，不阻塞调用线程。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return 异步结果；失败时以异常方式完成
     */
    public CompletableFuture<Boolean> chatCompletionWithSessionAsync(PromptRequest request, String sessionKey) {
        return httpClient.chatCompletionWithSessionAsync(request, sessionKey);
    }

    // ----------------------------------------------------------------
    // OpenAI 标准 ChatRequest/ChatResponse
    // ----------------------------------------------------------------

    /**
     * 同步提交聊天请求并返回完整响应。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param request 请求对象；不得为 {@code null}
     * @return OpenCode SDK 返回的聊天响应对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ChatResponse chatCompletion(String sessionId, ChatRequest request) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        PromptResult result = httpClient.prompt(sessionId, promptRequest);
        return ChatMessageMapper.toChatResponse(result);
    }

    /**
     * 异步完成指定会话的聊天请求。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param request 请求对象；不得为 {@code null}
     * @return 异步结果；失败时以异常方式完成
     */
    public CompletableFuture<ChatResponse> chatCompletionAsync(String sessionId, ChatRequest request) {
        return chatClient.chatCompletionAsync(sessionId, request);
    }

    /**
     * 复用或创建稳定会话后提交聊天请求并返回结果。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return OpenCode SDK 返回的聊天响应对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        PromptResult result = httpClient.chatCompletionWithSession(promptRequest, sessionKey);
        return ChatMessageMapper.toChatResponse(result);
    }

    /**
     * 复用或创建稳定会话后提交聊天请求并返回结果。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param cancellation 取消信号；为 {@code null} 时不可由外部取消
     * @return OpenCode SDK 返回的聊天响应对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        PromptResult result = httpClient.chatCompletionWithSession(promptRequest, sessionKey, cancellation);
        return ChatMessageMapper.toChatResponse(result);
    }

    /**
     * 异步查找会话并完成聊天请求。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param cancellation 取消信号；为 {@code null} 时不可由外部取消
     * @return 异步结果；失败时以异常方式完成
     */
    public CompletableFuture<ChatResponse> chatCompletionWithSessionAsync(ChatRequest request,
                                                                          String sessionKey,
                                                                          HttpCallCancellation cancellation) {
        return chatClient.chatCompletionWithSessionAsync(request, sessionKey, cancellation);
    }

    /**
     * 创建流式聊天响应，异步订阅会话事件并持续交付文本增量。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return 可观察文本增量、等待完成或主动取消的流式响应
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey) {
        return chatCompletionStream(request, sessionKey, null);
    }

    /**
     * 创建流式聊天响应，异步订阅会话事件并持续交付文本增量。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @return 可观察文本增量、等待完成或主动取消的流式响应
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context) {
        return chatCompletionStream(request, sessionKey, context, null);
    }

    /**
     * 流式对话，并在订阅启动前绑定增量回调，避免丢失首批分片。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @param deltaConsumer 文本增量消费者；不得为 {@code null}
     * @return 可观察文本增量、等待完成或主动取消的流式响应
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context,
                                                       Consumer<String> deltaConsumer) {
        if (Objects.isNull(chatClient)) {
            StreamingChatResponse stream = new StreamingChatResponse();
            stream.fail(new IllegalStateException("OpenCodeChatClient is not configured"));
            return stream;
        }
        return chatClient.chatCompletionStream(request, sessionKey, context, deltaConsumer);
    }

    /**
     * 查找稳定会话键对应的会话；不存在时创建新会话。
     *
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return 已复用或新建的 OpenCode 会话 ID
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public String ensureSession(String sessionKey) {
        return httpClient.ensureSession(sessionKey);
    }

    /**
     * 异步提交聊天请求；网络或解析失败通过返回的 Future 传播。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param request 请求对象；不得为 {@code null}
     * @return 异步结果；失败时以异常方式完成
     */
    public CompletableFuture<Boolean> chatCompletionAsync(String sessionId, PromptRequest request) {
        return httpClient.promptAsync(sessionId, request);
    }

    /**
     * 异步提交聊天请求；网络或解析失败通过返回的 Future 传播。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param text 发送给模型的文本内容
     * @return 异步结果；失败时以异常方式完成
     */
    public CompletableFuture<Boolean> chatCompletionAsync(String sessionId, String text) {
        return httpClient.promptAsync(sessionId, PromptRequest.ofText(text));
    }

    /**
     * 通过 OpenCode Server HTTP API 获取会话消息。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return OpenCode Server 返回的Prompt 结果列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<PromptResult> getMessages(String sessionId) {
        return httpClient.getMessages(sessionId);
    }

    /**
     * 请求 OpenCode Server 中止指定会话中正在运行的生成任务。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean abort(String sessionId) {
        return httpClient.abortSession(sessionId);
    }

    // ============================================================
    // Agent
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 查询智能体；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的智能体列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<Agent> listAgents() {
        return httpClient.listAgents();
    }

    // ============================================================
    // Global
    // ============================================================

    /**
     * 同步查询 OpenCode Server 的健康状态和版本信息。
     *
     * @return OpenCode SDK 返回的健康状态对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public HealthStatus health() {
        return httpClient.health();
    }

    /**
     * 异步检查 OpenCode Server 健康状态。
     *
     * @return 异步结果；失败时以异常方式完成
     */
    public CompletableFuture<HealthStatus> healthAsync() {
        return httpClient.healthAsync();
    }

    // ============================================================
    // SSE 事件流
    // ============================================================

    /**
     * 获取统一的 OpenCode 聊天场景客户端。
     *
     * @return OpenCode SDK 返回的聊天客户端对象
     */
    public OpenCodeChatClient chat() {
        return chatClient;
    }

    /**
     * 获取统一的 OpenCode SSE 场景客户端。
     *
     * @return OpenCode SDK 返回的SSE 客户端对象
     */
    public OpenCodeSseClient sse() {
        return sseClient;
    }

    // ============================================================
    // CLI
    // ============================================================

    /**
     * 委托本地 OpenCode CLI 门面执行OpenCode 协议数据，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 门面对象
     */
    public OpenCodeCli cli() {
        return cli;
    }

    // ============================================================
    // Config
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 获取实例配置。
     *
     * @return OpenCode SDK 返回的聚合客户端配置对象
     */
    public OpenCodeClientConfig getConfig() {
        return config;
    }

    /**
     * 通过 OpenCode Server HTTP API 获取配置。
     *
     * @return OpenCode SDK 返回的配置对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public OpenCodeConfig getOpenCodeConfig() {
        return httpClient.getConfig();
    }

    /**
     * 通过 OpenCode Server HTTP API 获取全局配置。
     *
     * @return OpenCode SDK 返回的配置对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public OpenCodeConfig getGlobalOpenCodeConfig() {
        return httpClient.getGlobalConfig();
    }

    /**
     * 向 OpenCode Server 提交配置更新，并返回更新后的服务端表示。
     *
     * @param body 提交给服务端的请求体
     * @return OpenCode SDK 返回的配置对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public OpenCodeConfig updateOpenCodeConfig(Object body) {
        return httpClient.updateConfig(body);
    }

    /**
     * 向 OpenCode Server 提交全局配置更新，并返回更新后的服务端表示。
     *
     * @param body 提交给服务端的请求体
     * @return OpenCode SDK 返回的配置对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public OpenCodeConfig updateGlobalOpenCodeConfig(Object body) {
        return httpClient.updateGlobalConfig(body);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取配置中的模型提供方。
     *
     * @return OpenCode SDK 返回的模型提供方清单对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ProviderList getConfigProviders() {
        return httpClient.getConfigProviders();
    }

    // ============================================================
    // Project
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 查询项目；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的项目列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<Project> listProjects() {
        return httpClient.listProjects();
    }

    /**
     * 通过 OpenCode Server HTTP API 获取当前项目。
     *
     * @return OpenCode SDK 返回的项目对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Project getCurrentProject() {
        return httpClient.getCurrentProject();
    }

    /**
     * 向 OpenCode Server 提交项目更新，并返回更新后的服务端表示。
     *
     * @param projectId OpenCode 项目 ID；不得为空
     * @param body 提交给服务端的请求体
     * @return OpenCode SDK 返回的项目对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Project updateProject(String projectId, Object body) {
        return httpClient.updateProject(projectId, body);
    }

    /**
     * 请求 OpenCode Server 在当前项目目录初始化 Git 仓库。
     *
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean initProjectGit() {
        return httpClient.initProjectGit();
    }

    // ============================================================
    // Provider
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 查询模型提供方；无数据时返回空集合。
     *
     * @return OpenCode SDK 返回的模型提供方清单对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ProviderList listProviders() {
        return httpClient.listProviders();
    }

    /**
     * 通过 OpenCode Server HTTP API 查询模型提供方认证方式；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的键值映射；无数据时为空映射
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Map<String, List<ProviderAuthMethod>> listProviderAuthMethods() {
        return httpClient.listProviderAuthMethods();
    }

    /**
     * 启动指定模型提供方的 OAuth 授权流程，并返回访问地址及校验状态。
     *
     * @param providerId 模型提供方 ID
     * @param method 认证或升级方式
     * @return OpenCode SDK 返回的模型提供方授权信息对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ProviderAuthAuthorization providerOAuthAuthorize(String providerId, String method) {
        return httpClient.providerOAuthAuthorize(providerId, method);
    }

    /**
     * 向 OpenCode Server 提交模型提供方 OAuth 回调授权码。
     *
     * @param providerId 模型提供方 ID
     * @param code OAuth 回调授权码
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean providerOAuthCallback(String providerId, String code) {
        return httpClient.providerOAuthCallback(providerId, code);
    }

    // ============================================================
    // File / Find
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 查询文件节点；无数据时返回空集合。
     *
     * @param path 文件或工作目录路径
     * @return OpenCode Server 返回的文件节点列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<FileNode> listFiles(String path) {
        return httpClient.listFiles(path);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取文件内容。
     *
     * @param path 文件或工作目录路径
     * @return OpenCode SDK 返回的文件内容对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public FileContent getFileContent(String path) {
        return httpClient.getFileContent(path);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取文件状态。
     *
     * @return OpenCode Server 返回的文件节点列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<FileNode> getFileStatus() {
        return httpClient.getFileStatus();
    }

    /**
     * 调用 OpenCode Server 搜索接口查找OpenCode 协议数据；未命中时返回空结果。
     *
     * @param pattern 文本搜索模式
     * @return OpenCode Server 返回的文件搜索匹配项列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<FileSearchResult> find(String pattern) {
        return httpClient.find(pattern);
    }

    /**
     * 调用 OpenCode Server 搜索接口查找文件节点；未命中时返回空结果。
     *
     * @param query 搜索或数据库查询表达式
     * @return OpenCode Server 返回的文本列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<String> findFiles(String query) {
        return httpClient.findFiles(query);
    }

    /**
     * 调用 OpenCode Server 搜索接口查找代码符号；未命中时返回空结果。
     *
     * @param query 搜索或数据库查询表达式
     * @return OpenCode Server 返回的代码符号列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<Symbol> findSymbols(String query) {
        return httpClient.findSymbols(query);
    }

    // ============================================================
    // Misc
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 查询斜杠命令；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的斜杠命令列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<Command> listCommands() {
        return httpClient.listCommands();
    }

    /**
     * 通过 OpenCode Server HTTP API 查询技能；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的技能描述列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<Skill> listSkills() {
        return httpClient.listSkills();
    }

    /**
     * 通过 OpenCode Server HTTP API 查询格式化器状态；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的格式化器状态列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<FormatterStatus> listFormatters() {
        return httpClient.listFormatters();
    }

    /**
     * 通过 OpenCode Server HTTP API 查询语言服务器状态；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的语言服务器状态列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<LspStatus> listLsps() {
        return httpClient.listLsps();
    }

    /**
     * 通过 OpenCode Server HTTP API 查询MCP 服务；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的键值映射；无数据时为空映射
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Map<String, McpStatus> listMcpServers() {
        return httpClient.listMcpServers();
    }

    /**
     * 向当前 OpenCode 实例注册 MCP 服务配置，并返回服务状态。
     *
     * @param name 资源名称
     * @param config 客户端配置；不得为 {@code null}
     * @return OpenCode SDK 返回的MCP 服务状态对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public McpStatus addMcpServer(String name, Object config) {
        return httpClient.addMcpServer(name, config);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取运行路径信息。
     *
     * @return OpenCode SDK 返回的运行路径对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public OpenCodePath getPath() {
        return httpClient.getPath();
    }

    /**
     * 通过 OpenCode Server HTTP API 获取版本控制状态。
     *
     * @return OpenCode SDK 返回的版本控制信息对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public VcsInfo getVcs() {
        return httpClient.getVcs();
    }

    // ============================================================
    // Session extended
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 获取会话状态。
     *
     * @return OpenCode Server 返回的键值映射；无数据时为空映射
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Map<String, SessionStatus> getSessionStatusMap() {
        return httpClient.getSessionStatusMap();
    }

    /**
     * 通过 OpenCode Server HTTP API 获取子会话。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return OpenCode Server 返回的会话列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<Session> getSessionChildren(String sessionId) {
        return httpClient.getSessionChildren(sessionId);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取会话待办项。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return OpenCode Server 返回的会话待办项列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<SessionTodo> getSessionTodo(String sessionId) {
        return httpClient.getSessionTodo(sessionId);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取会话文件差异。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param messageId 会话消息 ID
     * @return OpenCode Server 返回的文件差异列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<FileDiff> getSessionDiff(String sessionId, String messageId) {
        return httpClient.getSessionDiff(sessionId, messageId);
    }

    /**
     * 为指定会话启用共享，并返回更新后的会话信息。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return OpenCode SDK 返回的会话对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Session shareSession(String sessionId) {
        return httpClient.shareSession(sessionId);
    }

    /**
     * 取消指定会话的共享状态，并返回更新后的会话信息。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return OpenCode SDK 返回的会话对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Session unshareSession(String sessionId) {
        return httpClient.unshareSession(sessionId);
    }

    /**
     * 从指定消息位置派生新会话，并返回服务端创建的子会话。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param messageId 会话消息 ID
     * @return OpenCode SDK 返回的会话对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public Session forkSession(String sessionId, String messageId) {
        return httpClient.forkSession(sessionId, messageId);
    }

    /**
     * 使用指定消息和模型初始化会话上下文及 AGENTS.md。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param messageId 会话消息 ID
     * @param providerId 模型提供方 ID
     * @param modelId 模型 ID
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean initSession(String sessionId, String messageId, String providerId, String modelId) {
        return httpClient.initSession(sessionId, messageId, providerId, modelId);
    }

    /**
     * 使用指定模型压缩会话上下文，以降低后续请求的上下文长度。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param providerId 模型提供方 ID
     * @param modelId 模型 ID
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean summarizeSession(String sessionId, String providerId, String modelId) {
        return httpClient.summarizeSession(sessionId, providerId, modelId);
    }

    /**
     * 将会话回退到指定消息或消息片段。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param messageId 会话消息 ID
     * @param partId 消息片段 ID
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean revertSession(String sessionId, String messageId, String partId) {
        return httpClient.revertSession(sessionId, messageId, partId);
    }

    /**
     * 撤销会话最近一次回退操作。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean unrevertSession(String sessionId) {
        return httpClient.unrevertSession(sessionId);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取会话消息。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param messageId 会话消息 ID
     * @return OpenCode SDK 返回的消息详情对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public MessageInfo getMessage(String sessionId, String messageId) {
        return httpClient.getMessage(sessionId, messageId);
    }

    /**
     * 在指定会话中执行斜杠命令，并返回生成结果。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param command OpenCode 斜杠命令或 CLI 子命令
     * @param arguments 传递给斜杠命令的参数文本；为空时不附加参数
     * @param agent 执行请求的智能体名称；为空时使用服务端默认智能体
     * @param model 模型标识，通常采用 provider/model 格式；为空时使用默认模型
     * @return OpenCode SDK 返回的Prompt 结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public PromptResult runSessionCommand(String sessionId, String command, String arguments,
                                          String agent, String model) {
        return httpClient.runSessionCommand(sessionId, command, arguments, agent, model);
    }

    // ============================================================
    // Question
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 查询待回答问题；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的问题请求列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<QuestionRequest> listQuestions() {
        return httpClient.listQuestions();
    }

    /**
     * 回复question请求。
     *
     * @param requestId 待回复的问题或权限请求 ID
     * @param answers 按问题顺序提交的答案列表
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean replyQuestion(String requestId, List<String> answers) {
        return httpClient.replyQuestion(requestId, answers);
    }

    /**
     * 拒绝question请求。
     *
     * @param requestId 待回复的问题或权限请求 ID
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean rejectQuestion(String requestId) {
        return httpClient.rejectQuestion(requestId);
    }

    // ============================================================
    // Permission
    // ============================================================

    /**
     * 通过 OpenCode Server HTTP API 查询待处理权限；无数据时返回空集合。
     *
     * @return OpenCode Server 返回的权限请求列表；无数据时为空列表
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public List<PermissionRequest> listPermissions() {
        return httpClient.listPermissions();
    }

    /**
     * 回复permission请求。
     *
     * @param requestId 待回复的问题或权限请求 ID
     * @param response 权限请求响应值，例如 allow 或 deny
     * @param remember 是否记住本次权限决定并应用于后续同类请求
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean replyPermission(String requestId, String response, boolean remember) {
        return httpClient.replyPermission(requestId, response, remember);
    }

    // ============================================================
    // Auth
    // ============================================================

    /**
     * 保存指定模型提供方的认证配置。
     *
     * @param providerId 模型提供方 ID
     * @param body 提交给服务端的请求体
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean setAuth(String providerId, Object body) {
        return httpClient.setAuth(providerId, body);
    }

    /**
     * 请求 OpenCode Server 删除认证信息。
     *
     * @param providerId 模型提供方 ID
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean removeAuth(String providerId) {
        return httpClient.removeAuth(providerId);
    }

    // ============================================================
    // Instance / Global lifecycle
    // ============================================================

    /**
     * 释放当前 OpenCode Server 实例持有的运行资源。
     *
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean disposeInstance() {
        return httpClient.disposeInstance();
    }

    /**
     * 请求 OpenCode Server 释放全部全局实例资源。
     *
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean globalDispose() {
        return httpClient.globalDispose();
    }

    /**
     * 请求 OpenCode Server 将全局安装升级到指定目标版本。
     *
     * @param target 升级目标版本；为空时由 CLI 选择最新版本
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public boolean globalUpgrade(String target) {
        return httpClient.globalUpgrade(target);
    }

    // ============================================================
    // CLI facade
    // ============================================================

    /**
     * 委托本地 OpenCode CLI 门面执行{@code serve} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliServe() {
        return cli.serve(null, null);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code serve} 子命令，同步返回退出状态及输出。
     *
     * @param port 监听端口；为 {@code null} 时使用 CLI 默认值
     * @param hostname 监听地址；为空时使用 CLI 默认值
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliServe(Integer port, String hostname) {
        return cli.serve(port, hostname);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code web} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliWeb() {
        return cli.web(null, null);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code web} 子命令，同步返回退出状态及输出。
     *
     * @param port 监听端口；为 {@code null} 时使用 CLI 默认值
     * @param hostname 监听地址；为空时使用 CLI 默认值
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliWeb(Integer port, String hostname) {
        return cli.web(port, hostname);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code acp} 子命令，同步返回退出状态及输出。
     *
     * @param cwd CLI 进程使用的当前工作目录；为空时继承配置
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAcp(String cwd) {
        return cli.acp(cwd);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code generate} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliGenerate() {
        return cli.generate();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code attach} 子命令，同步返回退出状态及输出。
     *
     * @param url 远程服务或控制台 URL
     * @param dir CLI 命令作用目录；为空时使用配置工作目录
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param username OpenCode Server Basic Auth 用户名
     * @param password OpenCode Server Basic Auth 密码；日志中不得明文输出
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAttach(String url, String dir,
                                                                     String sessionId,
                                                                     String username, String password) {
        return cli.attach(url, dir, sessionId, username, password);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code upgrade} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliUpgrade() {
        return cli.upgrade();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code upgrade} 子命令，同步返回退出状态及输出。
     *
     * @param target 升级目标版本；为空时由 CLI 选择最新版本
     * @param method 认证或升级方式
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliUpgrade(String target, String method) {
        return cli.upgrade(target, method);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code uninstall} 子命令，同步返回退出状态及输出。
     *
     * @param keepConfig 卸载时是否保留本地配置
     * @param keepData 卸载时是否保留会话和缓存数据
     * @param dryRun 是否仅预览操作而不实际修改本地安装
     * @param force 是否强制执行操作
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliUninstall(boolean keepConfig,
                                                                         boolean keepData,
                                                                         boolean dryRun,
                                                                         boolean force) {
        return cli.uninstall(keepConfig, keepData, dryRun, force);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code stats} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliStats() {
        return cli.stats(null, null, null, null);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code stats} 子命令，同步返回退出状态及输出。
     *
     * @param days 统计覆盖的最近天数；为 {@code null} 时使用 CLI 默认范围
     * @param tools 统计结果中最多展示的工具数量；为 {@code null} 时使用 CLI 默认值
     * @param models 统计结果中最多展示的模型数量；为 {@code null} 时使用 CLI 默认值
     * @param project 统计限定的项目名称；为空时统计全部项目
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliStats(Integer days, Integer tools,
                                                                    Integer models, String project) {
        return cli.stats(days, tools, models, project);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code export} 子命令，同步返回退出状态及输出。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param sanitize 是否在导出结果中移除敏感信息
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliExport(String sessionId, boolean sanitize) {
        return cli.export(sessionId, sanitize);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code import} 子命令，同步返回退出状态及输出。
     *
     * @param fileOrUrl 待导入的本地文件路径或远程 URL
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliImport(String fileOrUrl) {
        return cli.importSession(fileOrUrl);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code session list} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliSessionList() {
        return cli.sessionList();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code session list} 子命令，同步返回退出状态及输出。
     *
     * @param maxCount 最大返回会话数量
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliSessionList(int maxCount) {
        return cli.sessionList(maxCount);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code agent list} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAgentList() {
        return cli.agentList();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code agent create} 子命令，同步返回退出状态及输出。
     *
     * @param path 文件或工作目录路径
     * @param description 资源的可读说明；为空时由 OpenCode 使用默认描述
     * @param mode 智能体运行模式或 CLI 行为模式
     * @param permissions 智能体创建时使用的权限配置文本
     * @param model 模型标识，通常采用 provider/model 格式；为空时使用默认模型
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAgentCreate(String path,
                                                                           String description,
                                                                           String mode,
                                                                           String permissions,
                                                                           String model) {
        return cli.agentCreate(path, description, mode, permissions, model);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code models} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliModels() {
        return cli.models();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code models} 子命令，同步返回退出状态及输出。
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @param verbose 是否输出模型的详细元数据
     * @param refresh 是否在列出模型前强制刷新提供方元数据
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliModels(String provider, boolean verbose,
                                                                    boolean refresh) {
        return cli.models(provider, verbose, refresh);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code providers list} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliProvidersList() {
        return cli.providersList();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code providers login} 子命令，同步返回退出状态及输出。
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @param method 认证或升级方式
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliProvidersLogin(String provider,
                                                                             String method) {
        return cli.providersLogin(provider, method);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code providers logout} 子命令，同步返回退出状态及输出。
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliProvidersLogout(String provider) {
        return cli.providersLogout(provider);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code auth list} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAuthList() {
        return cli.authList();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code auth login} 子命令，同步返回退出状态及输出。
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @param method 认证或升级方式
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAuthLogin(String provider, String method) {
        return cli.authLogin(provider, method);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code auth logout} 子命令，同步返回退出状态及输出。
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliAuthLogout(String provider) {
        return cli.authLogout(provider);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code mcp list} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliMcpList() {
        return cli.mcpList();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code mcp add} 子命令，同步返回退出状态及输出。
     *
     * @param name 资源名称
     * @param url 远程服务或控制台 URL
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliMcpAdd(String name, String url) {
        return cli.mcpAdd(name, url);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code mcp logout} 子命令，同步返回退出状态及输出。
     *
     * @param name 资源名称
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliMcpLogout(String name) {
        return cli.mcpLogout(name);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code mcp auth} 子命令，同步返回退出状态及输出。
     *
     * @param name 资源名称
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliMcpAuth(String name) {
        return cli.mcpAuth(name);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code db} 子命令，同步返回退出状态及输出。
     *
     * @param query 搜索或数据库查询表达式
     * @param format 输出格式名称；为空时使用 CLI 默认格式
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDb(String query, String format) {
        return cli.db(query, format);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code db path} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDbPath() {
        return cli.dbPath();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code debug config} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDebugConfig() {
        return cli.debugConfig();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code debug paths} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDebugPaths() {
        return cli.debugPaths();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code debug info} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliDebugInfo() {
        return cli.debugInfo();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code github install} 子命令，同步返回退出状态及输出。
     *
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliGithubInstall() {
        return cli.githubInstall();
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code github run} 子命令，同步返回退出状态及输出。
     *
     * @param event 触发当前回调的完整 SSE 事件
     * @param token GitHub 访问令牌；为空时由 CLI 自行解析
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliGithubRun(String event, String token) {
        return cli.githubRun(event, token);
    }

    /**
     * 委托本地 OpenCode CLI 门面执行{@code pr} 子命令，同步返回退出状态及输出。
     *
     * @param number GitHub Pull Request 编号
     * @return OpenCode SDK 返回的CLI 执行结果对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public io.github.easy4j.opencode.cli.OpenCodeCliResult cliPr(int number) {
        return cli.pr(number);
    }

    // ============================================================
    // 生命周期
    // ============================================================

    /**
     * 释放当前对象持有的连接、订阅或执行资源；重复调用是安全的。
     */
    @Override
    public void close() {
        if (sseClient != null) sseClient.close();
        if (httpClient != null) httpClient.close();
        OpenCodeOkHttpClientFactory.shutdown(ownedHttpClient);
    }
}
