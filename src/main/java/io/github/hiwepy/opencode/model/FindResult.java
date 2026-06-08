package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 文本搜索/查找结果（GET /find, GET /find/file）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FindResult {

    private String file;
    private int line;
    private int column;

    @JsonProperty("lineContent")
    private String lineContent;

    @JsonProperty("matchLength")
    private int matchLength;
}
