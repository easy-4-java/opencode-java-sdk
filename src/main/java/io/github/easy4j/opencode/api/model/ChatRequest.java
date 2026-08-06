package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions API 请求体（对齐 OpenClaw/Hermes）。
 * <p>
 * OpenCode 底层使用会话模型的 {@code PromptRequest}，SDK 内部自动转换：
 * {@code messages} 最后一条 user 消息 → {@code parts} text；
 * {@code model "provider/model"} → {@code ModelRef}。
 * </p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 2.7.x
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    /** 模型标识，格式 {@code provider/model}（如 {@code anthropic/claude-sonnet-4-5}）。 */
    private String model;

    /** 消息数组（OpenAI 标准格式）。 */
    private List<ChatMessage> messages;

    /** 是否启用 SSE 流式响应。 */
    private Boolean stream;

    /** 流式选项。 */
    private Map<String, Object> streamOptions;

    /** 指定 opencode agent 名称。 */
    private String agent;

    /** 系统 prompt 附加内容。 */
    private String system;

    /** 最大 token 数。 */
    private Integer maxTokens;

    /** 采样温度（0-2）。 */
    private Double temperature;

    /** nucleus 采样参数（0-1）。 */
    private Double topP;

    /** 用户标识。 */
    private String user;

    /** 快捷构造：单条 user 消息。 */
    public static ChatRequest ofUser(String content) {
        ChatRequest req = new ChatRequest();
        req.setMessages(Collections.singletonList(ChatMessage.user(content)));
        return req;
    }

    /** 快捷构造：单条 user 消息 + 模型。 */
    public static ChatRequest ofUser(String content, String model) {
        ChatRequest req = ofUser(content);
        req.setModel(model);
        return req;
    }
}
