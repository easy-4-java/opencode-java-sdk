package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 文件内容（GET /file/content）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileContent {

    private String path;
    private String content;
    private String language;

    @JsonProperty("lineCount")
    private int lineCount;
}
