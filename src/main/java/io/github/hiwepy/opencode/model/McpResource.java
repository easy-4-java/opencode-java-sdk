package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * MCP 资源信息（GET /experimental/resource）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpResource {

    private String uri;
    private String name;
    private String description;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("serverName")
    private String serverName;

    private Map<String, Object> metadata;
}
