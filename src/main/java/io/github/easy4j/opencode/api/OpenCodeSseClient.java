package io.github.easy4j.opencode.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.api.model.Event;
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
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Set<EventSource> eventSources = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
                eventSources.remove(es);
                log.info("SSE connection closed");
            }

            @Override
            public void onFailure(EventSource es, Throwable t, Response response) {
                eventSources.remove(es);
                if (response != null) {
                    log.warn("SSE connection failed, status={}", response.code(), t);
                } else {
                    log.warn("SSE connection failed", t);
                }
            }
        };
        EventSource eventSource = EventSources.createFactory(httpClient).newEventSource(request, listener);
        eventSources.add(eventSource);
        return eventSource;
    }

    /**
     * 阻塞式订阅，返回一个 BlockingQueue，事件入队供外部消费。
     */
    public BlockingQueue<Event> subscribeQueue() {
        return subscribeQueue(null);
    }

    public BlockingQueue<Event> subscribeQueue(OpenCodeRequestContext context) {
        return subscribeQueueSubscription(context).getQueue();
    }

    public QueueSubscription subscribeQueueSubscription(OpenCodeRequestContext context) {
        BlockingQueue<Event> queue = new ArrayBlockingQueue<>(
                Math.max(1, config.getStreamEventQueueCapacity()));
        EventSource source = subscribe(event -> offerLatest(queue, event), context);
        return new QueueSubscription(queue, source);
    }

    private void offerLatest(BlockingQueue<Event> queue, Event event) {
        if (!queue.offer(event)) {
            queue.poll();
            queue.offer(event);
            log.warn("OpenCode SSE event queue is full; discarded oldest event");
        }
    }

    /**
     * 订阅 SSE，仅消费指定 session 的事件。
     *
     * @param sessionId 目标 session ID（{@code null} 表示不过滤）
     * @param consumer  事件消费者（已按 sessionID 过滤）
     * @return EventSource
     */
    public EventSource subscribeSession(String sessionId, Consumer<Event> consumer) {
        return subscribeSession(sessionId, consumer, null);
    }

    public EventSource subscribeSession(String sessionId, Consumer<Event> consumer,
                                        OpenCodeRequestContext context) {
        return subscribe(filterBySession(sessionId, consumer), context);
    }

    /**
     * 订阅 SSE，仅消费指定事件类型集合中的事件。
     *
     * @param types     事件类型白名单（如 {@code "message.part.updated"}、{@code "session.idle"}）
     * @param consumer  事件消费者
     */
    public EventSource subscribeEventTypes(Set<String> types, Consumer<Event> consumer) {
        return subscribeEventTypes(types, consumer, null);
    }

    public EventSource subscribeEventTypes(Set<String> types, Consumer<Event> consumer,
                                           OpenCodeRequestContext context) {
        return subscribe(filterByTypes(types, consumer), context);
    }

    /**
     * 订阅 SSE，使用 {@link io.github.easy4j.opencode.api.event.EventHandler} 类型化回调。
     * 事件先按 sessionId 过滤（如果非 null），再分发到 handler 的对应方法。
     */
    public EventSource subscribeHandler(String sessionId,
                                        io.github.easy4j.opencode.api.event.EventHandler handler) {
        return subscribeHandler(sessionId, handler, null);
    }

    public EventSource subscribeHandler(String sessionId,
                                        io.github.easy4j.opencode.api.event.EventHandler handler,
                                        OpenCodeRequestContext context) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        return subscribe(filterBySession(sessionId, handler::onEvent), context);
    }

    private static Consumer<Event> filterBySession(String sessionId, Consumer<Event> delegate) {
        if (sessionId == null) {
            return delegate;
        }
        return event -> {
            if (event == null || event.getProperties() == null) {
                return;
            }
            Object sid = event.getProperties().get("sessionID");
            if (sessionId.equals(sid)) {
                delegate.accept(event);
            }
        };
    }

    private static Consumer<Event> filterByTypes(Set<String> types, Consumer<Event> delegate) {
        if (types == null || types.isEmpty()) {
            return delegate;
        }
        return event -> {
            if (event != null && event.getType() != null && types.contains(event.getType())) {
                delegate.accept(event);
            }
        };
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
        for (EventSource eventSource : eventSources) {
            eventSource.cancel();
        }
        eventSources.clear();
    }

    @Override
    public void close() {
        stop();
    }

    public static final class QueueSubscription implements AutoCloseable {

        private final BlockingQueue<Event> queue;
        private final EventSource eventSource;

        private QueueSubscription(BlockingQueue<Event> queue, EventSource eventSource) {
            this.queue = queue;
            this.eventSource = eventSource;
        }

        public BlockingQueue<Event> getQueue() {
            return queue;
        }

        @Override
        public void close() {
            eventSource.cancel();
        }
    }
}
