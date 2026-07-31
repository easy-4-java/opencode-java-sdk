package io.github.easy4j.opencode;

import lombok.Data;

/**
 * OpenCode HTTP Server 客户端配置。
 * <p>
 * 涵盖 Server 根地址、Basic Auth、TLS、HTTP 超时及默认模型等网络相关设置。
 * </p>
 */
@Data
public class OpenCodeHttpClientConfig {

    /**
     * OpenCode Server 根地址，例如 {@code http://localhost:4096}。
     */
    private String serverUrl = "http://localhost:4096";

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
