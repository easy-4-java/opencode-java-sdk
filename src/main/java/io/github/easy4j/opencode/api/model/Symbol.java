package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an LSP workspace symbol, returned by {@code GET /find/symbol}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#findSymbols(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Symbol {

    /**
     * 资源的可读名称。
     */
    private String name;

    /**
     * OpenCode 协议字段 {@code kind}；Java 类型为 {@code String}。
     */
    private String kind;

    /**
     * OpenCode 协议字段 {@code containerName}；Java 类型为 {@code String}。
     */
    @JsonProperty("containerName")
    private String containerName;

    /**
     * OpenCode 协议字段 {@code location}；Java 类型为 {@code String}。
     */
    private String location;

    /**
     * OpenCode 协议字段 {@code uri}；Java 类型为 {@code String}。
     */
    @JsonProperty("uri")
    private String uri;

    /**
     * OpenCode 协议字段 {@code range}；Java 类型为 {@code Object}。
     */
    @JsonProperty("range")
    private Object range;
}
