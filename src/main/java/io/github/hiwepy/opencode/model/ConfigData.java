package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 全局/项目配置（GET/PATCH /global/config, /config）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigData {

    @JsonProperty("defaultModel")
    private String defaultModel;

    @JsonProperty("defaultAgent")
    private String defaultAgent;

    @JsonProperty("autoCompact")
    private Boolean autoCompact;

    @JsonProperty("maxTokens")
    private Integer maxTokens;

    private Map<String, Object> extra;
}
