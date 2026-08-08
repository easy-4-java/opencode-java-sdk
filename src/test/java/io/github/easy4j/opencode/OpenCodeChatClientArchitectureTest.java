package io.github.easy4j.opencode;

import io.github.easy4j.opencode.api.OpenCodeChatClient;
import io.github.easy4j.opencode.api.OpenCodeHttpClient;
import io.github.easy4j.opencode.api.sse.StreamingChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenCodeChatClientArchitectureTest {

    @Test
    void shouldExposeUnifiedConfigAndChatScenarioClient() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        assertEquals(HttpResponseMode.BLOCKING, config.getMode());
        config.setBaseUrl("http://opencode");
        assertEquals("http://opencode", config.getBaseUrl());

        try (OpenCodeChatClient client = new OpenCodeChatClient(config)) {
            assertEquals(OpenCodeHttpClient.class, client.getClass().getSuperclass());
        }
    }

    @Test
    void shouldExposeStreamingResponseUnderSsePackage() {
        StreamingChatResponse response = new StreamingChatResponse();
        response.acceptDelta("hello");
        response.finish();
        assertEquals("hello", response.join());
    }
}
