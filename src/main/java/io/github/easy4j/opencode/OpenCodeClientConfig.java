package io.github.easy4j.opencode;

import lombok.Data;

/**
 * Unified configuration POJO for the OpenCode client.
 *
 * <p>Composes {@link OpenCodeHttpClientConfig} (HTTP/Server settings) and
 * {@link OpenCodeCliConfig} (local CLI settings). Compatible with Spring
 * {@code @ConfigurationProperties} binding.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeHttpClientConfig
 * @see OpenCodeCliConfig
 * @see OpenCodeClient
 */
@Data
public class OpenCodeClientConfig {

    /** HTTP/Server 相关配置 */
    private final OpenCodeHttpClientConfig http = new OpenCodeHttpClientConfig();

    /** 本地 CLI 相关配置 */
    private final OpenCodeCliConfig cli = new OpenCodeCliConfig();
}
