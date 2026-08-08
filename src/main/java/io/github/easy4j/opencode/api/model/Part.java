package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a part of an OpenCode message (text, tool_use, tool_result, etc.).
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see PromptRequest
 * @see PromptResult
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
