package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents VCS (Version Control System) information for the OpenCode working directory,
 * returned by {@code GET /vcs}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#getVcs()
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