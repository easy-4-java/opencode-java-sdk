package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 工作空间信息（GET/POST /experimental/workspace）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workspace {

    @JsonProperty("workspaceID")
    private String workspaceID;

    private String name;
    private String adapter;

    @JsonProperty("projectCount")
    private int projectCount;

    @JsonProperty("isSynced")
    private boolean synced;

    @JsonProperty("createdAt")
    private String createdAt;
}
