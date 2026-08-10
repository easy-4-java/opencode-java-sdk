package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Represents the status of an LSP server, returned by {@code GET /lsp}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listLsps()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LspStatus {

    /**
     * OpenCode Server 分配的唯一标识。
     */
    private String id;

    /**
     * 资源的可读名称。
     */
    private String name;

    /**
     * OpenCode 协议字段 {@code root}；Java 类型为 {@code String}。
     */
    private String root;

    /**
     * "running" / "starting" / "stopped" / "error"
     */
    private String status;

    /**
     * OpenCode 协议字段 {@code diagnostics} 的集合值；为空表示服务端未返回对应条目。
     */
    private List<String> diagnostics;
}
