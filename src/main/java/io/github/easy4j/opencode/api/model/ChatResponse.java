package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * OpenAI Chat Completions API response (aligned with OpenClaw/Hermes).
 * <p>OpenCode internally returns {@code PromptResult}; the SDK automatically converts:
 * {@code PromptResult.getTextContent()} is mapped to {@code choices[0].message.content}.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see ChatRequest
 * @see ChatMessage
 * @see io.github.easy4j.opencode.api.mapper.ChatMessageMapper
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {

    /**
     * 响应唯一标识。
     */
    private String id;

    /**
     * 对象类型。
     */
    private String object;

    /**
     * 创建时间戳（Unix epoch 秒）。
     */
    private Long created;

    /**
     * 使用的模型标识。
     */
    private String model;

    /**
     * 选择列表（通常只有一个元素）。
     */
    private List<Choice> choices;

    /**
     * Token 使用统计。
     */
    private Usage usage;

    /**
     * 响应中的一个选择。
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {

        /**
         * 选择在数组中的索引。
         */
        private Integer index;

        /**
         * agent 回复消息。
         */
        private ChatMessage message;

        /**
         * 完成原因：stop / length / tool_calls。
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * Token 使用统计。
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {

        /**
         * 输入提示消耗的 token 数量。
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 模型输出消耗的 token 数量。
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * 输入与输出合计 token 数量。
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * 快捷获取第一选择的消息内容。
     *
     * @return 服务端或 CLI 返回的文本值；无内容时可能为空字符串
     */
    public String getContent() {
        if (choices == null || choices.isEmpty() || choices.get(0).getMessage() == null) {
            return null;
        }
        return choices.get(0).getMessage().getContent();
    }
}
