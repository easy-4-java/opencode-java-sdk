package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * LSP 工作区符号，对应 {@code GET /find/symbol} 响应元素。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Symbol {

    private String name;

    private String kind;

    @JsonProperty("containerName")
    private String containerName;

    private String location;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("range")
    private Object range;
}