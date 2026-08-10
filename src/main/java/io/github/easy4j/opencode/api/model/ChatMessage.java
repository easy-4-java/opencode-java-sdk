package io.github.easy4j.opencode.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OpenAI Chat Completions API message object (aligned with OpenClaw/Hermes).
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see ChatRequest
 * @see ChatResponse
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    /** 消息角色：system / user / assistant / tool。 */
    private String role;

    /** 消息文本内容。 */
    private String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /** 快捷构造 user 消息。 */
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    /** 快捷构造 system 消息。 */
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    /** 快捷构造 assistant 消息。 */
    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
