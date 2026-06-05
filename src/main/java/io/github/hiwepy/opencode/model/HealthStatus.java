package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
