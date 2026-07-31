package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 文件 diff 项，对应 {@code GET /session/:id/diff} 响应元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileDiff {

    private String path;

    @JsonProperty("oldPath")
    private String oldPath;

    private String status;

    private Integer additions;

    private Integer deletions;

    private String patch;
}