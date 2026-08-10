package io.github.easy4j.opencode;

import io.github.easy4j.opencode.api.OpenCodeChatClient;
import io.github.easy4j.opencode.api.OpenCodeSseClient;
import io.github.easy4j.opencode.api.sse.SseSubscription;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 OpenCode 统一 SSE 对象拓扑和破坏性命名结果。 */
class OpenCodeSseApiShapeTest {

    @Test
    void shouldExposeOnlyTheUnifiedSseTopology() throws Exception {
        Method sseAccessor = OpenCodeClient.class.getMethod("sse");
        assertEquals(OpenCodeSseClient.class, sseAccessor.getReturnType());

        assertTrue(hasMethod(OpenCodeSseClient.class, "subscribeEvents"));
        assertTrue(hasMethod(OpenCodeSseClient.class, "subscribeSessionEvents"));
        assertTrue(hasMethod(OpenCodeSseClient.class, "activeSubscriptionCount"));
        assertFalse(hasMethod(OpenCodeSseClient.class, "subscribe"));
        assertFalse(hasMethod(OpenCodeSseClient.class, "subscribeSession"));
        assertFalse(hasMethod(OpenCodeSseClient.class, "subscribeQueue"));
        assertFalse(hasMethod(OpenCodeSseClient.class, "stop"));
        assertFalse(hasMethod(OpenCodeChatClient.class, "events"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.github.easy4j.opencode.api.model.Event"));

        for (Field field : OpenCodeChatClient.class.getDeclaredFields()) {
            assertFalse("eventClient".equals(field.getName()));
        }
    }

    @Test
    void shouldCancelSubscriptionIdempotently() {
        AtomicInteger cancellations = new AtomicInteger();
        SseSubscription subscription = new SseSubscription(cancellations::incrementAndGet);

        assertTrue(subscription.isActive());
        assertTrue(subscription.cancel());
        subscription.close();
        assertFalse(subscription.isActive());
        assertEquals(1, cancellations.get());
    }

    @Test
    void shouldExposeSseClientFromRootFacade() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setStartupCheckEnabled(false);
        try (OpenCodeClient client = new OpenCodeClient(config)) {
            assertNotNull(client.chat());
            assertNotNull(client.sse());
        }
    }

    private boolean hasMethod(Class<?> type, String name) {
        return Arrays.stream(type.getMethods()).map(Method::getName)
                .anyMatch(name::equals);
    }
}
