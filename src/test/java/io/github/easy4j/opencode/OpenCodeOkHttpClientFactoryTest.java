package io.github.easy4j.opencode;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link OpenCodeOkHttpClientFactory} 配置测试。
 */
class OpenCodeOkHttpClientFactoryTest {

    @Test
    void shouldApplyIndependentHttpConfiguration() {
        OpenCodeHttpClientConfig config = new OpenCodeHttpClientConfig();
        config.setConnectTimeoutMillis(11001);
        config.setReadTimeoutMillis(22002);
        config.setWriteTimeoutMillis(33003);
        config.setCallTimeoutMillis(44004);
        config.setMaxIdleConnections(13);
        config.setKeepAliveDurationMillis(55005L);
        config.setMaxRequests(66);
        config.setMaxRequestsPerHost(33);
        config.setRetryOnConnectionFailure(false);

        OkHttpClient client = OpenCodeOkHttpClientFactory.create(config);
        try {
            assertEquals(11001, client.connectTimeoutMillis());
            assertEquals(22002, client.readTimeoutMillis());
            assertEquals(33003, client.writeTimeoutMillis());
            assertEquals(44004, client.callTimeoutMillis());
            assertEquals(66, client.dispatcher().getMaxRequests());
            assertEquals(33, client.dispatcher().getMaxRequestsPerHost());
            assertFalse(client.retryOnConnectionFailure());
        } finally {
            OpenCodeOkHttpClientFactory.shutdown(client);
        }
    }
}
