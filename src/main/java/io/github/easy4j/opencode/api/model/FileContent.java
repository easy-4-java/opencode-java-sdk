package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 文件内容响应，对应 {@code GET /file/content}。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileContent {

    private String type;

    private String content;

    @JsonProperty("encoding")
    private String encoding;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("last_modified")
    private Long lastModified;

    @JsonProperty("size")
    private Long size;
}