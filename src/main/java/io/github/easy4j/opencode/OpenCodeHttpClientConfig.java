package io.github.easy4j.opencode;

import lombok.Data;

/**
 * Configuration for the OpenCode HTTP Server client.
 *
 * <p>Covers server base URL, Basic Auth, TLS, HTTP timeouts, connection pool sizing,
 * streaming thread pool, and default model/agent settings.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeHttpClient
 * @see OpenCodeOkHttpClientFactory
 */
@Data
public class OpenCodeHttpClientConfig {

    /** 对话响应模式，默认保持兼容的完整响应模式。 */
    private HttpResponseMode mode = HttpResponseMode.BLOCKING;

    /**
     * 是否启用 HTTP 子系统。
     * <p>为 false 时跳过 HTTP 客户端初始化和检查。</p>
     */
    private boolean enabled = true;

    /**
     * 启动时是否探测 HTTP 服务可用性（{@code GET /global/health}）。
     */
    private boolean startupCheckEnabled = false;

    /**
     * HTTP 服务不可用时是否快速失败（中断构造）。
     * <p>默认 false 仅打 WARN；生产环境建议设为 true。</p>
     */
    private boolean failFastOnUnavailable = false;

    /**
     * OpenCode Server 根地址，例如 {@code http://localhost:4096}。
     */
    private String baseUrl = "http://localhost:4096";

    /**
     * HTTP Basic Auth 用户名（对应 {@code OPENCODE_SERVER_USERNAME}，默认 {@code opencode}）。
     */
    private String username = "opencode";

    /**
     * HTTP Basic Auth 密码（对应 {@code OPENCODE_SERVER_PASSWORD}）。
     * <p>为空时不使用 Basic Auth。</p>
     */
    private String password;

    /**
     * 连接超时（毫秒）。
     */
    private int connectTimeoutMillis = 15_000;

    /**
     * 读取超时（毫秒）。
     * <p>opencode 的 prompt 请求可能耗时较长，建议设置较大值。</p>
     */
    private int readTimeoutMillis = 300_000;

    /**
     * 写入超时（毫秒）。
     */
    private int writeTimeoutMillis = 120_000;

    /**
     * 整个调用超时（毫秒）；0 表示不额外限制。
     */
    private int callTimeoutMillis;

    /**
     * 连接池最大空闲连接数。
     */
    private int maxIdleConnections = 32;

    /**
     * 空闲连接保活时间（毫秒）。
     */
    private long keepAliveDurationMillis = 300_000L;

    /**
     * 异步请求最大并发数。
     */
    private int maxRequests = 128;

    /**
     * 单主机异步请求最大并发数。
     */
    private int maxRequestsPerHost = 128;

    /** 流式事件处理线程数。 */
    private int streamCorePoolSize = 32;

    /** 流式事件处理最大线程数。 */
    private int streamMaxPoolSize = 32;

    /** 流式事件处理有界队列容量。 */
    private int streamQueueCapacity = 128;

    /** 流式事件处理线程空闲保活时间（毫秒）。 */
    private long streamKeepAliveMillis = 60_000L;

    /** 单个流式订阅的事件缓存上限。 */
    private int streamEventQueueCapacity = 1_024;

    /**
     * 遇到失效连接等传输故障时是否允许 OkHttp 自动恢复。
     */
    private boolean retryOnConnectionFailure = true;

    /**
     * 是否输出请求头、请求体及响应体等详细诊断信息。
     * <p>默认关闭；基础请求生命周期仍使用 DEBUG 日志。</p>
     */
    private boolean detailedLoggingEnabled = false;

    /** 详细日志中请求体、响应体的最大字符数。 */
    private int maxLoggedBodyLength = 2_000;

    /**
     * 是否校验 HTTPS 证书；为 false 时关闭校验（仅建议开发环境）。
     */
    private boolean verifySsl = true;

    /**
     * 默认使用的模型，格式 {@code provider/model}，例如 {@code anthropic/claude-sonnet-4-5}。
     * <p>为空时使用 opencode 服务端配置的默认模型。</p>
     */
    private String defaultModel;

    /**
     * 默认使用的 agent 名称。
     * <p>为空时使用 opencode 服务端配置的默认 agent。</p>
     */
    private String defaultAgent;

    /**
     * 解析用于 HTTP Basic Auth 的密码。
     *
     * @return password 非空则用之，否则空字符串
     */
    public String resolvePassword() {
        return password != null ? password : "";
    }
}
