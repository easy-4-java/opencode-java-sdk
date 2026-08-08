package io.github.easy4j.opencode.api.event;

import io.github.easy4j.opencode.api.model.Event;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EventHandler} default methods.
 */
class EventHandlerTest {

    @Test
    void shouldNotThrowOnDefaultMethods() {
        EventHandler handler = new EventHandler() {};
        Event event = new Event();
        event.setType("test");
        event.setProperties(Map.of());

        // All default methods should be no-op and not throw
        assertDoesNotThrow(() -> handler.onEvent(event));
        assertDoesNotThrow(() -> handler.onSessionIdle("sess-1", event));
        assertDoesNotThrow(() -> handler.onSessionError("sess-1", "error", event));
        assertDoesNotThrow(() -> handler.onTextDelta("delta", event));
        assertDoesNotThrow(() -> handler.onToolCall("bash", Map.of(), event));
        assertDoesNotThrow(() -> handler.onToolResult("use-1", "output", event));
        assertDoesNotThrow(() -> handler.onMessage("msg-1", "assistant", event));
        assertDoesNotThrow(() -> handler.onSessionStatus("sess-1", "idle", event));
        assertDoesNotThrow(() -> handler.onFileDiff("src/Main.java", event));
        assertDoesNotThrow(() -> handler.onPermissionRequested("sess-1", "perm-1", event));
        assertDoesNotThrow(() -> handler.onQuestionRequested("sess-1", "q-1", event));
    }
}
