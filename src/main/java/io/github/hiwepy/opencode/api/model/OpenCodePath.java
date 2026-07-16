package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenCode 工作目录相关路径，对应 {@code GET /path}。
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