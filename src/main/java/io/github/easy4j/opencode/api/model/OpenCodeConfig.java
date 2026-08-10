package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Represents the OpenCode global or instance configuration, returned by
 * {@code GET /config} and {@code GET /global/config}.
 *
 * <p>Fields are explicitly mapped via {@link JsonProperty} to support PATCH deserialization.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getConfig()
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getGlobalConfig()
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