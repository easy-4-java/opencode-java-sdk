package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents file content returned by {@code GET /file/content}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getFileContent(String)
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