package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an OpenCode project, returned by {@code GET /project} and {@code GET /project/current}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listProjects()
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getCurrentProject()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

    /**
     * OpenCode Server 分配的唯一标识。
     */
    private String id;

    /**
     * 资源的可读名称。
     */
    private String name;

    /**
     * OpenCode 协议字段 {@code icon}；Java 类型为 {@code String}。
     */
    private String icon;

    /**
     * OpenCode 协议字段 {@code worktree}；Java 类型为 {@code String}。
     */
    @JsonProperty("worktree")
    private String worktree;

    /**
     * OpenCode 协议字段 {@code vcsDir}；Java 类型为 {@code String}。
     */
    @JsonProperty("vcsDir")
    private String vcsDir;

    /**
     * OpenCode 协议字段 {@code vcs}；Java 类型为 {@code String}。
     */
    @JsonProperty("vcs")
    private String vcs;

    /**
     * OpenCode 协议字段 {@code sandboxes}；Java 类型为 {@code java.util.List<String>}。
     */
    @JsonProperty("sandboxes")
    private java.util.List<String> sandboxes;

    /**
     * 创建时间，由 OpenCode Server 返回。
     */
    @JsonProperty("created_at")
    private String createdAt;
}
