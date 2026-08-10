package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a file or directory node returned by {@code GET /file} or {@code GET /file/status}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listFiles(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileNode {

    private String path;

    /** "file" 或 "directory" */
    private String type;

    @JsonProperty("absolute")
    private String absolute;

    @JsonProperty("ignored")
    private Boolean ignored;
}