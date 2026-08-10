package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a part of an OpenCode message (text, tool_use, tool_result, etc.).
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see PromptRequest
 * @see PromptResult
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Part {

    /**
     * OpenCode 协议中的类型判别值。
     */
    private String type;
    /**
     * OpenCode 协议字段 {@code text}；Java 类型为 {@code String}。
     */
    private String text;

    /**
     * tool_use 的 tool name
     */
    private String name;

    /**
     * tool_use / tool_result 的 tool call id
     */
    @lombok.Getter
    @com.fasterxml.jackson.annotation.JsonProperty("tool_use_id")
    private String toolUseId;
}
