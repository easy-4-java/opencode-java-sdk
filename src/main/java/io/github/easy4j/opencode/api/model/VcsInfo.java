package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenCode 工作目录 VCS 信息，对应 {@code GET /vcs} 响应。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VcsInfo {

    private String branch;

    @JsonProperty("default_branch")
    private String defaultBranch;

    @JsonProperty("dirty")
    private Boolean dirty;

    @JsonProperty("ahead")
    private Integer ahead;

    @JsonProperty("behind")
    private Integer behind;
}