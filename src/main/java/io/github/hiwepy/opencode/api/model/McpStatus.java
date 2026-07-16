package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 状态，对应 {@code GET /mcp} 响应元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpStatus {

    private String name;

    /** "connected" / "disconnected" / "needs-auth" / "failed" */
    private String status;

    private Map<String, Object> config;

    private List<String> tools;
}