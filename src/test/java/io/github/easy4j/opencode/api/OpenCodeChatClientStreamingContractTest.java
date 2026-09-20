package io.github.easy4j.opencode.api;

import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.api.sse.SseEvent;
import io.github.easy4j.opencode.api.sse.StreamingChatResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenCodeChatClientStreamingContractTest {

    @Test
    void messagePartDeltaMustAppendTextExactlyOnce() throws Exception {
        OpenCodeChatClient client = new OpenCodeChatClient(new OpenCodeHttpClientConfig());
        try {
            StreamingChatResponse stream = new StreamingChatResponse();
            SseEvent event = event("message.part.delta", "sess-1");
            event.getProperties().put("field", "text");
            event.getProperties().put("delta", "hello");

            invokeHandleEvent(client, "sess-1", event, stream);

            assertEquals("hello", stream.getAccumulatedContent());
        } finally {
            client.close();
        }
    }

    @Test
    void messagePartUpdatedSnapshotMustNotBeAppendedAsDelta() throws Exception {
        OpenCodeChatClient client = new OpenCodeChatClient(new OpenCodeHttpClientConfig());
        try {
            StreamingChatResponse stream = new StreamingChatResponse();
            SseEvent event = event("message.part.updated", "sess-1");
            Map<String, Object> part = new HashMap<String, Object>();
            part.put("type", "text");
            part.put("text", "whole snapshot");
            event.getProperties().put("part", part);

            invokeHandleEvent(client, "sess-1", event, stream);

            assertEquals("", stream.getAccumulatedContent());
        } finally {
            client.close();
        }
    }

    private static SseEvent event(String type, String sessionId) {
        SseEvent event = new SseEvent();
        event.setType(type);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("sessionID", sessionId);
        event.setProperties(properties);
        return event;
    }

    private static void invokeHandleEvent(OpenCodeChatClient client, String sessionId,
                                          SseEvent event, StreamingChatResponse stream) throws Exception {
        Method method = OpenCodeChatClient.class.getDeclaredMethod(
                "handleEvent", String.class, SseEvent.class, StreamingChatResponse.class);
        method.setAccessible(true);
        method.invoke(client, sessionId, event, stream);
    }
}
