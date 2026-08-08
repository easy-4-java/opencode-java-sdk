package io.github.easy4j.opencode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeCliConfig}.
 */
class OpenCodeCliConfigTest {

    @Test
    void shouldHaveCorrectDefaults() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        assertTrue(config.isEnabled());
        assertFalse(config.isStartupCheckEnabled());
        assertFalse(config.isFailFastOnUnavailable());
        assertEquals("opencode", config.getExecutable());
        assertEquals(300, config.getTimeout());
        assertEquals(5, config.getProbeTimeoutSeconds());
        assertNull(config.getWorkingDirectory());
        assertEquals(0, config.getMaxConcurrentExecutions());
    }

    @Test
    void shouldAllowCustomization() {
        OpenCodeCliConfig config = new OpenCodeCliConfig();
        config.setEnabled(false);
        config.setStartupCheckEnabled(true);
        config.setFailFastOnUnavailable(true);
        config.setExecutable("/usr/local/bin/opencode");
        config.setTimeout(600);
        config.setProbeTimeoutSeconds(10);
        config.setWorkingDirectory("/data/project");
        config.setMaxConcurrentExecutions(4);

        assertFalse(config.isEnabled());
        assertTrue(config.isStartupCheckEnabled());
        assertTrue(config.isFailFastOnUnavailable());
        assertEquals("/usr/local/bin/opencode", config.getExecutable());
        assertEquals(600, config.getTimeout());
        assertEquals(10, config.getProbeTimeoutSeconds());
        assertEquals("/data/project", config.getWorkingDirectory());
        assertEquals(4, config.getMaxConcurrentExecutions());
    }
}
