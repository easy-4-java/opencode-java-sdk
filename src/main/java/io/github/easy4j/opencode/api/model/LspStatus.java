package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Represents the status of an LSP server, returned by {@code GET /lsp}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listLsps()
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