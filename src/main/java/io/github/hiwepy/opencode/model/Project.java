package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 项目信息（GET /project）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

    @JsonProperty("projectID")
    private String projectID;

    private String name;
    private String directory;

    @JsonProperty("workspaceID")
    private String workspaceID;

    @JsonProperty("sessionCount")
    private int sessionCount;

    @JsonProperty("createdAt")
    private String createdAt;

    private Map<String, Object> metadata;
}
