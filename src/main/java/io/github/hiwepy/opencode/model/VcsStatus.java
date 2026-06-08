package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * VCS（Git）状态信息（GET /vcs/status）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VcsStatus {

    @JsonProperty("isRepo")
    private boolean repo;

    private String branch;

    @JsonProperty("remoteURL")
    private String remoteURL;

    @JsonProperty("changedFiles")
    private int changedFiles;

    @JsonProperty("stagedFiles")
    private int stagedFiles;

    @JsonProperty("untrackedFiles")
    private int untrackedFiles;

    @JsonProperty("isClean")
    private boolean clean;
}
