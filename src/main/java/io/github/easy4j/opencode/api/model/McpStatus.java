package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents the status of an MCP server, returned by {@code GET /mcp}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listMcpServers()
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