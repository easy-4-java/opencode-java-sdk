package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * opencode server 健康检查响应。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthStatus {

    private Boolean healthy;
    private String version;
}
