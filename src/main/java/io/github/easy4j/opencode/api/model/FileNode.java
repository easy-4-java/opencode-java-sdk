package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a file or directory node returned by {@code GET /file} or {@code GET /file/status}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#listFiles(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileNode {

    /**
     * 相对于项目根目录的文件路径。
     */
    private String path;

    /**
     * "file" 或 "directory"
     */
    private String type;

    /**
     * 文件在本机文件系统中的绝对路径。
     */
    @JsonProperty("absolute")
    private String absolute;

    /**
     * 文件是否被版本控制忽略规则排除。
     */
    @JsonProperty("ignored")
    private Boolean ignored;
}
