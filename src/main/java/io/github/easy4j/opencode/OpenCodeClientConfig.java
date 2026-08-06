package io.github.easy4j.opencode;

import lombok.Data;

/**
 * OpenCode 客户端统一配置（纯 POJO，可与 Spring {@code @ConfigurationProperties} 映射）。
 * <p>
 * 组合 {@link OpenCodeHttpClientConfig}（HTTP/Server 相关）与 {@link OpenCodeCliConfig}（本地 CLI 相关），
 * 作为 {@link OpenCodeClient} 等统一入口的配置载体。
 * </p>
 */
@Data
public class OpenCodeClientConfig {

    /** HTTP/Server 相关配置 */
    private final OpenCodeHttpClientConfig http = new OpenCodeHttpClientConfig();

    /** 本地 CLI 相关配置 */
    private final OpenCodeCliConfig cli = new OpenCodeCliConfig();
}
