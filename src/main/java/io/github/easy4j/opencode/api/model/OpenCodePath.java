package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents OpenCode working directory paths, returned by {@code GET /path}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getPath()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenCodePath {

    private String home;

    private String state;

    private String config;

    private String directory;

    private String worktree;

    @JsonProperty("worktree_dir")
    private String worktreeDir;
}