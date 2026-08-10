package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents the status of an MCP server, returned by {@code GET /mcp}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listMcpServers()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpStatus {

    /**
     * 资源的可读名称。
     */
    private String name;

    /**
     * "connected" / "disconnected" / "needs-auth" / "failed"
     */
    private String status;

    /**
     * 当前客户端使用的不可变配置引用。
     */
    private Map<String, Object> config;

    /**
     * 工具启用状态及工具级配置映射。
     */
    private List<String> tools;
}
