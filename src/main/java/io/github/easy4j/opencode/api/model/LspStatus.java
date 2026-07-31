package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * LSP Server 状态，对应 {@code GET /lsp} 响应元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LspStatus {

    private String id;

    private String name;

    private String root;

    /** "running" / "starting" / "stopped" / "error" */
    private String status;

    private List<String> diagnostics;
}