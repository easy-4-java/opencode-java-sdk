package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工具信息（GET /experimental/tool, GET /experimental/tool/ids）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolInfo {

    private String name;
    private String description;

    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;

    @JsonProperty("isAgentTool")
    private boolean agentTool;

    @JsonProperty("providerID")
    private String providerID;

    @JsonProperty("modelID")
    private String modelID;
}
