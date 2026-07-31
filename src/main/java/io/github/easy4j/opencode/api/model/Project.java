package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenCode Project，对应 {@code GET /project} / {@code GET /project/current} 响应。
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