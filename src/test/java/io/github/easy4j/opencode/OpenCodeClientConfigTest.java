package io.github.easy4j.opencode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeClientConfig}.
 */
class OpenCodeClientConfigTest {

    @Test
    void shouldInitializeWithDefaultSubConfigs() {
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        assertNotNull(config.getHttp());
        assertNotNull(config.getCli());
        assertSame(config.getDebug(), config.getHttp().getDebug());
        assertSame(config.getDebug(), config.getCli().getDebug());
        assertTrue(config.getHttp().isEnabled());
        assertTrue(config.getCli().isEnabled());
    }

    @Test
    void shouldShareSubConfigReferences() {
        OpenCodeClientConfig config = new OpenCodeClientConfig();
        config.getHttp().setBaseUrl("http://custom:8080");
        assertEquals("http://custom:8080", config.getHttp().getBaseUrl());

        config.getCli().setExecutable("my-opencode");
        assertEquals("my-opencode", config.getCli().getExecutable());
    }
}
