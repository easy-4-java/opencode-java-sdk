package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a file diff entry returned by {@code GET /session/:id/diff}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getSessionDiff(String, String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileDiff {

    private String path;

    @JsonProperty("oldPath")
    private String oldPath;

    private String status;

    private Integer additions;

    private Integer deletions;

    private String patch;
}