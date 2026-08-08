package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Represents a ripgrep text search result returned by {@code GET /find}.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient#find(String)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileSearchResult {

    private String path;

    /** 匹配所在行内容 */
    private List<String> lines;

    @JsonProperty("line_number")
    private Integer lineNumber;

    @JsonProperty("absolute_offset")
    private Integer absoluteOffset;

    /** 子匹配（命中片段） */
    private List<Object> submatches;
}