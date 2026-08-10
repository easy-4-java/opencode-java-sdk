package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents file content returned by {@code GET /file/content}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getFileContent(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileContent {

    /**
     * OpenCode 协议中的类型判别值。
     */
    private String type;

    /**
     * 按到达顺序累计的流式文本内容。
     */
    private String content;

    /**
     * 文件内容编码，例如 utf-8 或 base64。
     */
    @JsonProperty("encoding")
    private String encoding;

    /**
     * 文件 MIME 类型。
     */
    @JsonProperty("mimeType")
    private String mimeType;

    /**
     * 文件最后修改时间，由服务端协议表示。
     */
    @JsonProperty("last_modified")
    private Long lastModified;

    /**
     * 文件大小，单位由 OpenCode Server 协议定义。
     */
    @JsonProperty("size")
    private Long size;
}
