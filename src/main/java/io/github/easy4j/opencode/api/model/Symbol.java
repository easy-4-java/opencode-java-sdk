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
     * 语言服务器返回的符号类别，例如类、方法或字段。
     */
    private String kind;

    /**
     * 包含该符号的类、命名空间或其他容器名称。
     */
    @JsonProperty("containerName")
    private String containerName;

    /**
     * 符号所在文件及范围的兼容位置描述。
     */
    private String location;

    /**
     * 符号源文件的 URI。
     */
    @JsonProperty("uri")
    private String uri;

    /**
     * 语言服务器返回的起止行列范围对象。
     */
    @JsonProperty("range")
    private Object range;
}
