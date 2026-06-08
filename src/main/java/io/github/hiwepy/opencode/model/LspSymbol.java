package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * LSP 符号搜索/状态（GET /find/symbol, GET /lsp）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LspSymbol {

    private String name;
    private String kind;
    private String file;
    private int line;
    private int column;

    @JsonProperty("containerName")
    private String containerName;
}
