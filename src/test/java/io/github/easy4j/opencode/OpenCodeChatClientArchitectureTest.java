package io.github.easy4j.opencode;

import io.github.easy4j.opencode.api.OpenCodeChatClient;
import io.github.easy4j.opencode.api.OpenCodeHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenCodeChatClientArchitectureTest {

    @Test
    void shouldExposeUnifiedConfigAndChatScenarioClient() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        assertEquals(HttpResponseMode.BLOCKING, config.getMode());
        config.setServerUrl("http://legacy-opencode");
        assertEquals("http://legacy-opencode", config.getBaseUrl());

        try (OpenCodeChatClient client = new OpenCodeChatClient(config)) {
            assertEquals(OpenCodeHttpClient.class, client.getClass().getSuperclass());
        }
    }
}
