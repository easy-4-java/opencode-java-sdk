package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents VCS (Version Control System) information for the OpenCode working directory,
 * returned by {@code GET /vcs}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getVcs()
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VcsInfo {

    /**
     * 当前版本控制分支名称。
     */
    private String branch;

    /**
     * 仓库默认分支名称。
     */
    @JsonProperty("default_branch")
    private String defaultBranch;

    /**
     * 工作区是否包含未提交修改。
     */
    @JsonProperty("dirty")
    private Boolean dirty;

    /**
     * 当前分支领先上游的提交数量。
     */
    @JsonProperty("ahead")
    private Integer ahead;

    /**
     * 当前分支落后上游的提交数量。
     */
    @JsonProperty("behind")
    private Integer behind;
}
