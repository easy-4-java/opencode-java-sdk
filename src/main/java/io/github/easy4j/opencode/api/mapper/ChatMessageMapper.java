package io.github.easy4j.opencode.api.mapper;

import io.github.easy4j.opencode.api.model.ChatMessage;
import io.github.easy4j.opencode.api.model.ChatRequest;
import io.github.easy4j.opencode.api.model.ChatResponse;
import io.github.easy4j.opencode.api.model.PromptRequest;
import io.github.easy4j.opencode.api.model.PromptResult;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * {@link ChatRequest}/{@link ChatResponse}（OpenAI 标准）与 {@link PromptRequest}/{@link PromptResult}（OpenCode 会话模型）互转。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class ChatMessageMapper {

    private ChatMessageMapper() {
    }

    /**
     * ChatRequest → PromptRequest。
     * <p>
     * 取最后一条 user 消息的 content 作为 text part；model "provider/model" 拆分为 ModelRef；
     * agent / system 直传。
     * </p>
     *
     * @param chatRequest OpenAI 风格的聊天请求
     * @return OpenCode SDK 返回的Prompt 请求对象
     */
    public static PromptRequest toPromptRequest(ChatRequest chatRequest) {
        if (chatRequest == null) {
            return null;
        }

        // 取最后一条 user 消息内容
        String text = extractUserContent(chatRequest.getMessages());

        PromptRequest promptRequest = PromptRequest.ofText(text);

        // model: "provider/model" → ModelRef
        if (chatRequest.getModel() != null && chatRequest.getModel().contains("/")) {
            String[] parts = chatRequest.getModel().split("/", 2);
            promptRequest.setModel(new PromptRequest.ModelRef(parts[0], parts[1]));
        }

        // agent
        if (chatRequest.getAgent() != null) {
            promptRequest.setAgent(chatRequest.getAgent());
        }

        // system prompt
        if (chatRequest.getSystem() != null) {
            promptRequest.setSystem(chatRequest.getSystem());
        }

        return promptRequest;
    }

    /**
     * PromptResult → ChatResponse。
     * <p>getTextContent() → choices[0].message.content。</p>
     *
     * @param result OpenCode Prompt 执行结果
     * @return OpenCode SDK 返回的聊天响应对象
     */
    public static ChatResponse toChatResponse(PromptResult result) {
        if (result == null) {
            return null;
        }

        ChatResponse response = new ChatResponse();
        response.setId(UUID.randomUUID().toString());
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);

        String content = result.getTextContent();

        ChatMessage message = new ChatMessage("assistant", content);

        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason("stop");

        response.setChoices(Collections.singletonList(choice));

        return response;
    }

    /**
     * 从消息列表中提取最后一条 user 消息的 content。
     */
    private static String extractUserContent(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        // 从后往前找最后一条 user 消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if ("user".equals(msg.getRole()) && msg.getContent() != null) {
                return msg.getContent();
            }
        }
        // 兜底：取最后一条消息
        ChatMessage last = messages.get(messages.size() - 1);
        return last.getContent() != null ? last.getContent() : "";
    }
}
