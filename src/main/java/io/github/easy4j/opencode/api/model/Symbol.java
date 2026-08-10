package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an LSP workspace symbol, returned by {@code GET /find/symbol}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#findSymbols(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Symbol {

    private String name;

    private String kind;

    @JsonProperty("containerName")
    private String containerName;

    private String location;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("range")
    private Object range;
}