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
     * 消息片段类型，例如 {@code text}、{@code tool_use} 或 {@code tool_result}。
     */
    private String type;
    /**
     * 文本类型消息片段的正文；非文本片段可能为空。
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
