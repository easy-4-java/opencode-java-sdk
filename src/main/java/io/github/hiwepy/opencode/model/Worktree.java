package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Worktree 信息（GET/POST /experimental/worktree）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Worktree {

    private String name;
    private String path;
    private String branch;

    @JsonProperty("baseBranch")
    private String baseBranch;

    @JsonProperty("isActive")
    private boolean active;
}
