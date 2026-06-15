package io.github.hiwepy.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * OpenCode Message Part（消息的组成部分：text, tool_use, tool_result 等）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Part {

    private String type;
    private String text;

    /** tool_use 的 tool name */
    private String name;

    /** tool_use / tool_result 的 tool call id */
    @lombok.Getter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("tool_use_id"))
    private String toolUseId;
}
