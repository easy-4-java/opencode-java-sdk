package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 文件/目录节点，对应 {@code GET /file} 响应元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileNode {

    private String path;

    /** "file" 或 "directory" */
    private String type;

    @JsonProperty("absolute")
    private String absolute;

    @JsonProperty("ignored")
    private Boolean ignored;
}