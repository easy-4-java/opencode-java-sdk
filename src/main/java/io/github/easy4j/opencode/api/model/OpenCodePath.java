package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents OpenCode working directory paths, returned by {@code GET /path}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getPath()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenCodePath {

    /**
     * OpenCode 协议字段 {@code home}；Java 类型为 {@code String}。
     */
    private String home;

    /**
     * OAuth 防重放状态值。
     */
    private String state;

    /**
     * 当前客户端使用的不可变配置引用。
     */
    private String config;

    /**
     * OpenCode 协议字段 {@code directory}；Java 类型为 {@code String}。
     */
    private String directory;

    /**
     * OpenCode 协议字段 {@code worktree}；Java 类型为 {@code String}。
     */
    private String worktree;

    /**
     * OpenCode 协议字段 {@code worktreeDir}；Java 类型为 {@code String}。
     */
    @JsonProperty("worktree_dir")
    private String worktreeDir;
}
