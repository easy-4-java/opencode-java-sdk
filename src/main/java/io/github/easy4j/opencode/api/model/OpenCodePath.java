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
     * 当前用户的主目录路径。
     */
    private String home;

    /**
     * OpenCode 持久化运行状态目录。
     */
    private String state;

    /**
     * 当前客户端使用的不可变配置引用。
     */
    private String config;

    /**
     * 当前 OpenCode Server 实例的工作目录。
     */
    private String directory;

    /**
     * 当前项目工作树根目录。
     */
    private String worktree;

    /**
     * 服务端返回的工作树目录兼容字段。
     */
    @JsonProperty("worktree_dir")
    private String worktreeDir;
}
