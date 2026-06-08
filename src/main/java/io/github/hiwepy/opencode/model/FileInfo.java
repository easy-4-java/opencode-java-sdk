package io.github.hiwepy.opencode.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 文件/目录信息（GET /file）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileInfo {

    private String name;
    private String path;
    private String type;  // "file" or "directory"
    private long size;

    @com.fasterxml.jackson.annotation.JsonProperty("modifiedAt")
    private String modifiedAt;
}
