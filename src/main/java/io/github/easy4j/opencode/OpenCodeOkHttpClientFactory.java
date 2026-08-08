package io.github.easy4j.opencode;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating {@link OkHttpClient} instances used by the OpenCode SDK in standalone mode.
 *
 * <p>When a Spring container already provides an external {@link OkHttpClient}, the injection
 * constructors should be used instead and this factory will not participate.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see OpenCodeHttpClientConfig
 */
public final class OpenCodeOkHttpClientFactory {

    private OpenCodeOkHttpClientFactory() {
    }

    /**
     * 根据 OpenCode HTTP 配置创建客户端。
     *
     * @param config HTTP 配置
     * @return SDK 自主管理的 OkHttpClient
     */
    public static OkHttpClient create(OpenCodeHttpClientConfig config) {
        Objects.requireNonNull(config, "config");
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(Math.max(1, config.getMaxRequests()));
        dispatcher.setMaxRequestsPerHost(Math.max(1, config.getMaxRequestsPerHost()));
        ConnectionPool connectionPool = new ConnectionPool(
                Math.max(1, config.getMaxIdleConnections()),
                Math.max(1L, config.getKeepAliveDurationMillis()),
                TimeUnit.MILLISECONDS);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .connectTimeout(Math.max(1, config.getConnectTimeoutMillis()), TimeUnit.MILLISECONDS)
                .readTimeout(Math.max(0, config.getReadTimeoutMillis()), TimeUnit.MILLISECONDS)
                .writeTimeout(Math.max(1, config.getWriteTimeoutMillis()), TimeUnit.MILLISECONDS)
                .callTimeout(Math.max(0, config.getCallTimeoutMillis()), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(config.isRetryOnConnectionFailure());
        if (!config.isVerifySsl()) {
            builder.hostnameVerifier((hostname, session) -> true);
        }
        return builder.build();
    }

    /**
     * 释放 SDK 自建客户端资源。
     *
     * @param client SDK 自建客户端
     */
    public static void shutdown(OkHttpClient client) {
        if (Objects.isNull(client)) {
            return;
        }
        client.dispatcher().cancelAll();
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdown();
    }
}
