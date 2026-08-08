package io.github.easy4j.opencode.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeSessionKeys}.
 */
class OpenCodeSessionKeysTest {

    @Test
    void shouldCreateStableSessionKey() {
        String key = OpenCodeSessionKeys.forStableSession("agent1", "user1");
        assertEquals("opencode:agent1:user1", key);
    }

    @Test
    void shouldCreateStableSessionKeyWithChannel() {
        String key = OpenCodeSessionKeys.forStableSession("agent1", "user1", "xiaohongshu");
        assertEquals("opencode:agent1:user1:xiaohongshu", key);
    }

    @Test
    void shouldNormalizeSegmentsToLowerCase() {
        String key = OpenCodeSessionKeys.forStableSession("Agent1", "User1");
        assertEquals("opencode:agent1:user1", key);
    }

    @Test
    void shouldTrimSegments() {
        String key = OpenCodeSessionKeys.forStableSession(" agent1 ", " user1 ");
        assertEquals("opencode:agent1:user1", key);
    }

    @Test
    void shouldRejectNullAgentId() {
        assertThrows(NullPointerException.class,
                () -> OpenCodeSessionKeys.forStableSession(null, "user1"));
    }

    @Test
    void shouldRejectNullPeerId() {
        assertThrows(NullPointerException.class,
                () -> OpenCodeSessionKeys.forStableSession("agent1", null));
    }

    @Test
    void shouldRejectBlankAgentId() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenCodeSessionKeys.forStableSession("  ", "user1"));
    }

    @Test
    void shouldRejectBlankPeerId() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenCodeSessionKeys.forStableSession("agent1", "  "));
    }

    @Test
    void shouldRejectColonInSegment() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenCodeSessionKeys.forStableSession("agent:1", "user1"));
    }

    @Test
    void shouldRejectIllegalCharacters() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenCodeSessionKeys.forStableSession("agent@1", "user1"));
    }

    @Test
    void shouldAcceptUnderscoreAndDotAndDash() {
        String key = OpenCodeSessionKeys.forStableSession("my_agent-1.test", "user_2");
        assertEquals("opencode:my_agent-1.test:user_2", key);
    }

    @Test
    void shouldRejectNullChannel() {
        assertThrows(NullPointerException.class,
                () -> OpenCodeSessionKeys.forStableSession("agent1", "user1", null));
    }

    @Test
    void shouldRejectBlankChannel() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenCodeSessionKeys.forStableSession("agent1", "user1", "  "));
    }

    @Test
    void shouldRejectColonInChannel() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenCodeSessionKeys.forStableSession("agent1", "user1", "ch:1"));
    }
}
