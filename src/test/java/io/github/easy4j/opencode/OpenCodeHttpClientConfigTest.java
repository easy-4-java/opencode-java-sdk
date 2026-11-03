package io.github.easy4j.opencode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeHttpClientConfig}.
 */
class OpenCodeHttpClientConfigTest {

    @Test
    void shouldHaveCorrectDefaults() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        assertEquals(HttpResponseMode.BLOCKING, config.getMode());
        assertTrue(config.isEnabled());
        assertFalse(config.isStartupCheckEnabled());
        assertFalse(config.isFailFastOnUnavailable());
        assertEquals("http://localhost:4096", config.getServerUrl());
        assertEquals("opencode", config.getUsername());
        assertNull(config.getPassword());
        assertEquals(15_000, config.getConnectTimeoutMillis());
        assertEquals(300_000, config.getReadTimeoutMillis());
        assertEquals(120_000, config.getWriteTimeoutMillis());
        assertEquals(0, config.getCallTimeoutMillis());
        assertEquals(32, config.getMaxIdleConnections());
        assertEquals(300_000L, config.getKeepAliveDurationMillis());
        assertEquals(128, config.getMaxRequests());
        assertEquals(128, config.getMaxRequestsPerHost());
        assertEquals(32, config.getStreamCorePoolSize());
        assertEquals(32, config.getStreamMaxPoolSize());
        assertEquals(128, config.getStreamQueueCapacity());
        assertEquals(60_000L, config.getStreamKeepAliveMillis());
        assertEquals(1_024, config.getStreamEventQueueCapacity());
        assertTrue(config.isRetryOnConnectionFailure());
        assertTrue(config.isVerifySsl());
        assertNull(config.getDefaultModel());
        assertNull(config.getDefaultAgent());
    }

    @Test
    void shouldResolvePasswordWhenPresent() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setPassword("secret");
        assertEquals("secret", config.resolvePassword());
    }

    @Test
    void shouldResolveEmptyPasswordWhenNull() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        assertNull(config.getPassword());
        assertEquals("", config.resolvePassword());
    }

    @Test
    void shouldKeepLegacySseEventQueueAlias() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setSseEventQueueCapacity(17);
        assertEquals(17, config.getStreamEventQueueCapacity());
    }

    @Test
    void aliasesShareCanonicalState() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setServerUrl("http://legacy:4096");
        assertEquals("http://legacy:4096", config.getBaseUrl());
        config.setBaseUrl("http://canonical:4096");
        assertEquals("http://canonical:4096", config.getServerUrl());
        config.setStreamEventQueueCapacity(23);
        assertEquals(23, config.getSseEventQueueCapacity());
        config.setSseEventQueueCapacity(31);
        assertEquals(31, config.getStreamEventQueueCapacity());
    }

    @Test
    void jsonBindsBothPropertyNamesAndRoundTrips() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String names : new String[]{"serverUrl,sseEventQueueCapacity", "baseUrl,streamEventQueueCapacity"}) {
            String[] properties = names.split(",");
            String json = "{\"" + properties[0] + "\":\"http://json:4096\",\"" + properties[1] + "\":37}";
            OpenCodeHttpClientConfig config = mapper.readValue(json, OpenCodeHttpClientConfig.class);
            assertEquals("http://json:4096", config.getBaseUrl());
            assertEquals(config.getBaseUrl(), config.getServerUrl());
            assertEquals(37, config.getStreamEventQueueCapacity());
            assertEquals(37, config.getSseEventQueueCapacity());
            assertEquals(config, mapper.readValue(mapper.writeValueAsString(config), OpenCodeHttpClientConfig.class));
        }
    }
}
