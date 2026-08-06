package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * OpenAI Chat Completions API 响应（对齐 OpenClaw/Hermes）。
 * <p>
 * OpenCode 底层返回 {@code PromptResult}，SDK 内部自动转换：
 * {@code PromptResult.getTextContent()} → {@code choices[0].message.content}。
 * </p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 2.7.x
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {

    /** 响应唯一标识。 */
    private String id;

    /** 对象类型。 */
    private String object;

    /** 创建时间戳（Unix epoch 秒）。 */
    private Long created;

    /** 使用的模型标识。 */
    private String model;

    /** 选择列表（通常只有一个元素）。 */
    private List<Choice> choices;

    /** Token 使用统计。 */
    private Usage usage;

    /**
     * 响应中的一个选择。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {

        /** 选择在数组中的索引。 */
        private Integer index;

        /** agent 回复消息。 */
        private ChatMessage message;

        /** 完成原因：stop / length / tool_calls。 */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * Token 使用统计。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {

        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * 快捷获取第一选择的消息内容。
     */
    public String getContent() {
        if (choices == null || choices.isEmpty() || choices.get(0).getMessage() == null) {
            return null;
        }
        return choices.get(0).getMessage().getContent();
    }
}
