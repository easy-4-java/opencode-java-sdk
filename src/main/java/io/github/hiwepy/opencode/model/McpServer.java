package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务器状态（GET /mcp）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpServer {

    private String name;
    private String status;
    private String type;

    @JsonProperty("toolCount")
    private int toolCount;

    @JsonProperty("resourceCount")
    private int resourceCount;

    @JsonProperty("connectedAt")
    private String connectedAt;

    private Map<String, Object> config;
    private List<String> tools;
}
