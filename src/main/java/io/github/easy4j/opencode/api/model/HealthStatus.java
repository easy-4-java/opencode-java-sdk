package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents the health check response from the OpenCode Server ({@code GET /global/health}).
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#health()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthStatus {

    /**
     * 服务端是否通过健康检查。
     */
    private Boolean healthy;
    /**
     * OpenCode Server 或 CLI 版本字符串。
     */
    private String version;
}
