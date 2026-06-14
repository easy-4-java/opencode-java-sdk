package io.github.hiwepy.opencode;

import io.github.hiwepy.opencode.cli.OpenCodeCli;
import io.github.hiwepy.opencode.cli.OpenCodeCliExecutor;
import io.github.hiwepy.opencode.http.OpenCodeHttpClient;
import io.github.hiwepy.opencode.http.OpenCodeSseClient;
import io.github.hiwepy.opencode.model.*;

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
     * 标准构造（自动创建 HTTP、SSE、CLI 客户端）。
     */
    public OpenCodeClient(OpenCodeClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = new OpenCodeHttpClient(config);
        this.sseClient = new OpenCodeSseClient(config);
        this.cli = new OpenCodeCli(new OpenCodeCliExecutor(config));
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
     * 建议用 {@link io.github.hiwepy.opencode.api.OpenCodeSessionKeys} 生成 sessionKey。</p>
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

    /**
     * 确保指定 sessionKey 对应的 session 存在，返回其 sessionId。
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
    // 生命周期
    // ============================================================

    @Override
    public void close() {
        httpClient.close();
        sseClient.close();
    }
}
