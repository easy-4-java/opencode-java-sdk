package io.github.easy4j.opencode.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeRequestContext}.
 */
class OpenCodeRequestContextTest {

    @Test
    void shouldCreateContextWithDirectory() {
        OpenCodeRequestContext ctx = OpenCodeRequestContext.ofDirectory("/data/projects/foo");
        assertEquals("/data/projects/foo", ctx.getDirectory());
    }

    @Test
    void shouldBuildContextViaBuilder() {
        OpenCodeRequestContext ctx = OpenCodeRequestContext.builder()
                .directory("/data/projects/bar")
                .build();
        assertEquals("/data/projects/bar", ctx.getDirectory());
    }

    @Test
    void shouldHandleNullDirectory() {
        OpenCodeRequestContext ctx = OpenCodeRequestContext.ofDirectory(null);
        assertNull(ctx.getDirectory());
    }
}
