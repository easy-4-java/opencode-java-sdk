package io.github.easy4j.opencode.api.mapper;

import io.github.easy4j.opencode.api.model.ChatMessage;
import io.github.easy4j.opencode.api.model.ChatRequest;
import io.github.easy4j.opencode.api.model.ChatResponse;
import io.github.easy4j.opencode.api.model.PromptRequest;
import io.github.easy4j.opencode.api.model.PromptResult;
import io.github.easy4j.opencode.api.model.Part;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChatMessageMapper}.
 */
class ChatMessageMapperTest {

    @Test
    void shouldReturnNullWhenChatRequestIsNull() {
        assertNull(ChatMessageMapper.toPromptRequest(null));
    }

    @Test
    void shouldConvertChatRequestToPromptRequest() {
        ChatRequest req = new ChatRequest();
        req.setMessages(Collections.singletonList(ChatMessage.user("hello")));
        req.setModel("anthropic/claude-sonnet-4-5");
        req.setAgent("coder");
        req.setSystem("system prompt");

        PromptRequest result = ChatMessageMapper.toPromptRequest(req);

        assertNotNull(result);
        assertNotNull(result.getParts());
        assertEquals(1, result.getParts().size());
        assertEquals("text", result.getParts().get(0).getType());
        assertEquals("hello", result.getParts().get(0).getText());
        assertNotNull(result.getModel());
        assertEquals("anthropic", result.getModel().getProviderID());
        assertEquals("claude-sonnet-4-5", result.getModel().getModelID());
        assertEquals("coder", result.getAgent());
        assertEquals("system prompt", result.getSystem());
    }

    @Test
    void shouldExtractLastUserMessage() {
        ChatRequest req = new ChatRequest();
        req.setMessages(Arrays.asList(
                ChatMessage.user("first"),
                ChatMessage.assistant("reply"),
                ChatMessage.user("last message")
        ));

        PromptRequest result = ChatMessageMapper.toPromptRequest(req);
        assertEquals("last message", result.getParts().get(0).getText());
    }

    @Test
    void shouldHandleNullMessages() {
        ChatRequest req = new ChatRequest();
        req.setMessages(null);
        PromptRequest result = ChatMessageMapper.toPromptRequest(req);
        assertNotNull(result);
        assertEquals("", result.getParts().get(0).getText());
    }

    @Test
    void shouldHandleEmptyMessages() {
        ChatRequest req = new ChatRequest();
        req.setMessages(Collections.emptyList());
        PromptRequest result = ChatMessageMapper.toPromptRequest(req);
        assertNotNull(result);
        assertEquals("", result.getParts().get(0).getText());
    }

    @Test
    void shouldFallbackToLastMessageWhenNoUserMessage() {
        ChatRequest req = new ChatRequest();
        req.setMessages(Arrays.asList(
                ChatMessage.system("system"),
                ChatMessage.assistant("assistant")
        ));

        PromptRequest result = ChatMessageMapper.toPromptRequest(req);
        assertEquals("assistant", result.getParts().get(0).getText());
    }

    @Test
    void shouldHandleModelWithoutSlash() {
        ChatRequest req = new ChatRequest();
        req.setMessages(Collections.singletonList(ChatMessage.user("hi")));
        req.setModel("claude-sonnet-4-5");

        PromptRequest result = ChatMessageMapper.toPromptRequest(req);
        assertNull(result.getModel());
    }

    @Test
    void shouldReturnNullWhenPromptResultIsNull() {
        assertNull(ChatMessageMapper.toChatResponse(null));
    }

    @Test
    void shouldConvertPromptResultToChatResponse() {
        Part textPart = new Part();
        textPart.setType("text");
        textPart.setText("Hello, world!");
        PromptResult promptResult = new PromptResult();
        promptResult.setParts(Collections.singletonList(textPart));

        ChatResponse response = ChatMessageMapper.toChatResponse(promptResult);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("chat.completion", response.getObject());
        assertNotNull(response.getCreated());
        assertEquals(1, response.getChoices().size());
        assertEquals(0, response.getChoices().get(0).getIndex());
        assertEquals("assistant", response.getChoices().get(0).getMessage().getRole());
        assertEquals("Hello, world!", response.getChoices().get(0).getMessage().getContent());
        assertEquals("stop", response.getChoices().get(0).getFinishReason());
    }

    @Test
    void shouldHandleNullContentInLastMessage() {
        ChatRequest req = new ChatRequest();
        ChatMessage msg = new ChatMessage("user", null);
        req.setMessages(Collections.singletonList(msg));

        PromptRequest result = ChatMessageMapper.toPromptRequest(req);
        assertEquals("", result.getParts().get(0).getText());
    }
}
