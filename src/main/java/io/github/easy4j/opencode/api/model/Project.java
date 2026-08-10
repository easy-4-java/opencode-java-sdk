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
     * 项目图标标识或图标资源地址。
     */
    private String icon;

    /**
     * 项目主工作目录路径。
     */
    @JsonProperty("worktree")
    private String worktree;

    /**
     * 版本控制元数据目录路径。
     */
    @JsonProperty("vcsDir")
    private String vcsDir;

    /**
     * 项目使用的版本控制系统类型。
     */
    @JsonProperty("vcs")
    private String vcs;

    /**
     * 项目关联的沙箱目录列表。
     */
    @JsonProperty("sandboxes")
    private java.util.List<String> sandboxes;

    /**
     * 创建时间，由 OpenCode Server 返回。
     */
    @JsonProperty("created_at")
    private String createdAt;
}
