package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Represents a ripgrep text search result returned by {@code GET /find}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#find(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileSearchResult {

    /**
     * 相对于项目根目录的文件路径。
     */
    private String path;

    /**
     * 匹配所在行内容
     */
    private List<String> lines;

    /**
     * 匹配所在的从一开始计数的行号。
     */
    @JsonProperty("line_number")
    private Integer lineNumber;

    /**
     * 匹配内容相对于文件开头的绝对偏移量。
     */
    @JsonProperty("absolute_offset")
    private Integer absoluteOffset;

    /**
     * 子匹配（命中片段）
     */
    private List<Object> submatches;
}
