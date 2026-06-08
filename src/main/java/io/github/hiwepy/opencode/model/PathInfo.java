package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 工作目录信息（GET /path）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PathInfo {

    private String directory;

    @JsonProperty("isProject")
    private boolean project;

    @JsonProperty("projectName")
    private String projectName;

    @JsonProperty("worktreePath")
    private String worktreePath;
}
