package io.github.hiwepy.opencode.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.OpenCodeHttpClientConfig;
import io.github.hiwepy.opencode.api.model.Event;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenCode Server SSE 客户端，消费 {@code GET /event} 事件流。
 * <p>基于 OkHttp {@link EventSources}，支持外部传入 {@link OkHttpClient}。</p>
 */
@Slf4j
public class OpenCodeSseClient implements AutoCloseable {

    private final OpenCodeHttpClientConfig config;
    private final ObjectMapper mapper;
    private final OkHttpClient httpClient;
    private volatile EventSource eventSource;

    public OpenCodeSseClient(OpenCodeHttpClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.config = config;
        this.mapper = Objects.isNull(objectMapper) ? new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) : objectMapper;
        this.httpClient = Objects.isNull(httpClient) ? buildOkHttpClient(config) : httpClient;
    }

    private static OkHttpClient buildOkHttpClient(OpenCodeHttpClientConfig config) {
        // 兜底创建（SSE 需要无读超时）
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS);
        if (!config.isVerifySsl()) {
            builder.hostnameVerifier((hostname, session) -> true);
        }
        return builder.build();
    }

    /**
     * 订阅 SSE 事件流，事件通过 consumer 回调。
     * <p>OkHttp {@link EventSources} 内部用守护线程处理，无需自建 ExecutorService。</p>
     *
     * @param consumer 事件消费者
     * @return EventSource（可用于 cancel）
     */
    public EventSource subscribe(Consumer<Event> consumer) {
        return subscribe(consumer, null);
    }

    public EventSource subscribe(Consumer<Event> consumer, OpenCodeRequestContext context) {
        Request request = buildRequest(context);
        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource es, Response response) {
                log.info("SSE connected to {}/event", config.getServerUrl());
            }

            @Override
            public void onEvent(EventSource es, String id, String type, String data) {
                if (data != null && !data.isEmpty()) {
                    try {
                        Event event = mapper.readValue(data, Event.class);
                        consumer.accept(event);
                    } catch (Exception e) {
                        log.debug("Failed to parse SSE event: {}", data, e);
                    }
                }
            }

            @Override
            public void onClosed(EventSource es) {
                log.info("SSE connection closed");
            }

            @Override
            public void onFailure(EventSource es, Throwable t, Response response) {
                if (response != null) {
                    log.warn("SSE connection failed, status={}", response.code(), t);
                } else {
                    log.warn("SSE connection failed", t);
                }
            }
        };
        this.eventSource = EventSources.createFactory(httpClient).newEventSource(request, listener);
        return this.eventSource;
    }

    /**
     * 阻塞式订阅，返回一个 BlockingQueue，事件入队供外部消费。
     */
    public BlockingQueue<Event> subscribeQueue() {
        return subscribeQueue(null);
    }

    public BlockingQueue<Event> subscribeQueue(OpenCodeRequestContext context) {
        BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
        subscribe(queue::add, context);
        return queue;
    }

    private Request buildRequest(OpenCodeRequestContext context) {
        String url = config.getServerUrl() + "/event";
        Request.Builder builder = new Request.Builder().url(url)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache");
        String password = config.resolvePassword();
        if (!password.isEmpty()) {
            builder.header("Authorization", Credentials.basic(config.getUsername(), password));
        }
        if (Objects.nonNull(context) && context.getDirectory() != null
                && !context.getDirectory().trim().isEmpty()) {
            builder.header("X-OpenCode-Directory", context.getDirectory());
        }
        return builder.build();
    }

    /**
     * 停止事件流订阅。
     */
    public void stop() {
        if (eventSource != null) {
            eventSource.cancel();
            eventSource = null;
        }
    }

    @Override
    public void close() {
        stop();
    }
}
