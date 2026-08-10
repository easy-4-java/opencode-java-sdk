package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an OpenCode project, returned by {@code GET /project} and {@code GET /project/current}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listProjects()
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getCurrentProject()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

    private String id;

    private String name;

    private String icon;

    @JsonProperty("worktree")
    private String worktree;

    @JsonProperty("vcsDir")
    private String vcsDir;

    @JsonProperty("vcs")
    private String vcs;

    @JsonProperty("sandboxes")
    private java.util.List<String> sandboxes;

    @JsonProperty("created_at")
    private String createdAt;
}