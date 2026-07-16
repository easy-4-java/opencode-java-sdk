package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * OpenCode 全局/实例 Config，对应 {@code GET /config} 与 {@code GET /global/config}。
 * <p>字段全部以 {@link JsonProperty} 显式映射，便于 PATCH 反序列化。</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenCodeConfig {

    private String theme;
    private String model;
    private String agent;
    private String provider;

    @JsonProperty("default_agent")
    private String defaultAgent;

    @JsonProperty("default_model")
    private String defaultModel;

    private String username;

    @JsonProperty("share")
    private String share;

    @JsonProperty("autoshare")
    private Boolean autoshare;

    @JsonProperty("auto_share")
    private Boolean autoShare;

    private Map<String, Object> mode;

    private Map<String, Object> provider_;

    private Map<String, Object> providers;

    private Map<String, Object> agent_;

    private Map<String, Object> agents;

    private Map<String, Object> permission;

    private Map<String, Object> tools;

    private Map<String, Object> experimental;

    /** 兜底字段：未知字段落这里 */
    private Map<String, Object> extra;
}