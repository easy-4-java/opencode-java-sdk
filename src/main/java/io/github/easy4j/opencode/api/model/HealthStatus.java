package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents the health check response from the OpenCode Server ({@code GET /global/health}).
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#health()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthStatus {

    private Boolean healthy;
    private String version;
}
