package io.github.easy4j.opencode.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.OpenCodeOkHttpClientFactory;
import io.github.easy4j.opencode.api.event.EventHandler;
import io.github.easy4j.opencode.api.sse.SseEvent;
import io.github.easy4j.opencode.api.sse.SseQueueSubscription;
import io.github.easy4j.opencode.api.sse.SseSubscription;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * OpenCode Server SSE 客户端，负责事件订阅、过滤、取消和资源回收。
 */
@Slf4j
public class OpenCodeSseClient implements AutoCloseable {

    private final OpenCodeHttpClientConfig config;
    private final ObjectMapper mapper;
    private final OkHttpClient httpClient;
    private final boolean ownsHttpClient;
    private final Set<SseSubscription> activeSubscriptions = ConcurrentHashMap.newKeySet();

    /** 使用 SDK 自建 OkHttpClient 创建 SSE 客户端。 */
    public OpenCodeSseClient(OpenCodeHttpClientConfig config) {
        this(config, new ObjectMapper(), null);
    }

    /** 使用共享 ObjectMapper 和 OkHttpClient 创建 SSE 客户端。 */
    public OpenCodeSseClient(OpenCodeHttpClientConfig config, ObjectMapper objectMapper,
                             OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.mapper = Objects.isNull(objectMapper) ? new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) : objectMapper;
        this.ownsHttpClient = Objects.isNull(httpClient);
        OkHttpClient baseClient = ownsHttpClient
                ? OpenCodeOkHttpClientFactory.create(config) : httpClient;
        this.httpClient = baseClient.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build();
        log.debug("OpenCode SSE client initialized: baseUrl={}, maxRequests={}, "
                        + "maxRequestsPerHost={}, eventQueueCapacity={}, reconnectPolicy=none, "
                        + "detailedLoggingEnabled={}",
                config.getBaseUrl(), config.getMaxRequests(), config.getMaxRequestsPerHost(),
                config.getStreamEventQueueCapacity(), config.isDetailedLoggingEnabled());
    }

    /** 订阅全局事件流。 */
    public SseSubscription subscribeEvents(Consumer<SseEvent> consumer) {
        return subscribeEvents(consumer, null);
    }

    /** 使用请求上下文订阅全局事件流。 */
    public SseSubscription subscribeEvents(Consumer<SseEvent> consumer,
                                           OpenCodeRequestContext context) {
        Objects.requireNonNull(consumer, "consumer");
        Request request = buildRequest(context);
        AtomicReference<EventSource> eventSourceRef = new AtomicReference<>();
        AtomicReference<SseSubscription> subscriptionRef = new AtomicReference<>();
        SseSubscription subscription = new SseSubscription(() -> {
            EventSource eventSource = eventSourceRef.get();
            if (Objects.nonNull(eventSource)) {
                eventSource.cancel();
            }
            SseSubscription current = subscriptionRef.get();
            if (Objects.nonNull(current)) {
                activeSubscriptions.remove(current);
            }
        });
        subscriptionRef.set(subscription);
        activeSubscriptions.add(subscription);
        long startedAt = System.nanoTime();
        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                log.info("OpenCode SSE connected: streamType=events, url={}, status={}, elapsedMs={}",
                        request.url(), response.code(), elapsedMillis(startedAt));
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                SseSubscription subscription = subscriptionRef.get();
                if (Objects.isNull(subscription) || !subscription.isActive()
                        || Objects.isNull(data) || data.isEmpty()) {
                    return;
                }
                try {
                    consumer.accept(mapper.readValue(data, SseEvent.class));
                } catch (Exception error) {
                    if (config.isDetailedLoggingEnabled()) {
                        log.debug("Failed to parse OpenCode SSE event: data={}", data, error);
                    } else {
                        log.debug("Failed to parse OpenCode SSE event: dataLength={}, error={}",
                                data.length(), error.getMessage());
                    }
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                closeSubscription(subscriptionRef);
                log.info("OpenCode SSE closed: streamType=events, url={}", request.url());
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable error, Response response) {
                closeSubscription(subscriptionRef);
                log.warn("OpenCode SSE failed: streamType=events, url={}, status={}, error={}",
                        request.url(), Objects.nonNull(response) ? response.code() : -1,
                        Objects.nonNull(error) ? error.getMessage() : "unknown");
            }
        };
        EventSource eventSource = EventSources.createFactory(httpClient)
                .newEventSource(request, listener);
        eventSourceRef.set(eventSource);
        if (!subscription.isActive()) {
            eventSource.cancel();
        }
        return subscription;
    }

    /** 创建带有界队列的全局事件订阅。 */
    public SseQueueSubscription subscribeEventsQueue(OpenCodeRequestContext context) {
        BlockingQueue<SseEvent> queue = new ArrayBlockingQueue<>(
                Math.max(1, config.getStreamEventQueueCapacity()));
        SseSubscription subscription = subscribeEvents(event -> offerLatest(queue, event), context);
        return new SseQueueSubscription(queue, subscription);
    }

    /** 订阅指定 session 的事件。 */
    public SseSubscription subscribeSessionEvents(String sessionId,
                                                  Consumer<SseEvent> consumer) {
        return subscribeSessionEvents(sessionId, consumer, null);
    }

    /** 使用请求上下文订阅指定 session 的事件。 */
    public SseSubscription subscribeSessionEvents(String sessionId,
                                                  Consumer<SseEvent> consumer,
                                                  OpenCodeRequestContext context) {
        return subscribeEvents(filterBySession(sessionId, consumer), context);
    }

    /** 使用类型化处理器订阅指定 session 的事件。 */
    public SseSubscription subscribeSessionEvents(String sessionId, EventHandler handler) {
        return subscribeSessionEvents(sessionId, handler, null);
    }

    /** 使用请求上下文和类型化处理器订阅指定 session 的事件。 */
    public SseSubscription subscribeSessionEvents(String sessionId, EventHandler handler,
                                                  OpenCodeRequestContext context) {
        Objects.requireNonNull(handler, "handler");
        return subscribeEvents(filterBySession(sessionId, handler::onEvent), context);
    }

    /** 订阅指定类型集合中的事件。 */
    public SseSubscription subscribeEventTypes(Set<String> types, Consumer<SseEvent> consumer) {
        return subscribeEventTypes(types, consumer, null);
    }

    /** 使用请求上下文订阅指定类型集合中的事件。 */
    public SseSubscription subscribeEventTypes(Set<String> types, Consumer<SseEvent> consumer,
                                               OpenCodeRequestContext context) {
        return subscribeEvents(filterByTypes(types, consumer), context);
    }

    /** 返回当前活动订阅数量。 */
    public int activeSubscriptionCount() {
        return activeSubscriptions.size();
    }

    private Consumer<SseEvent> filterBySession(String sessionId, Consumer<SseEvent> delegate) {
        Objects.requireNonNull(delegate, "delegate");
        if (Objects.isNull(sessionId)) {
            return delegate;
        }
        return event -> {
            if (Objects.nonNull(event) && Objects.nonNull(event.getProperties())
                    && Objects.equals(sessionId, event.getProperties().get("sessionID"))) {
                delegate.accept(event);
            }
        };
    }

    private Consumer<SseEvent> filterByTypes(Set<String> types, Consumer<SseEvent> delegate) {
        Objects.requireNonNull(delegate, "delegate");
        if (Objects.isNull(types) || types.isEmpty()) {
            return delegate;
        }
        return event -> {
            if (Objects.nonNull(event) && Objects.nonNull(event.getType())
                    && types.contains(event.getType())) {
                delegate.accept(event);
            }
        };
    }

    private void offerLatest(BlockingQueue<SseEvent> queue, SseEvent event) {
        if (!queue.offer(event)) {
            queue.poll();
            queue.offer(event);
            log.warn("OpenCode SSE event queue is full; discarded oldest event");
        }
    }

    private Request buildRequest(OpenCodeRequestContext context) {
        Request.Builder builder = new Request.Builder().url(config.getBaseUrl() + "/event")
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache");
        String password = config.resolvePassword();
        if (!password.isEmpty()) {
            builder.header("Authorization", Credentials.basic(config.getUsername(), password));
        }
        if (Objects.nonNull(context) && Objects.nonNull(context.getDirectory())
                && !context.getDirectory().trim().isEmpty()) {
            builder.header("X-OpenCode-Directory", context.getDirectory());
        }
        return builder.build();
    }

    private void closeSubscription(AtomicReference<SseSubscription> subscriptionRef) {
        SseSubscription subscription = subscriptionRef.get();
        if (Objects.nonNull(subscription)) {
            subscription.close();
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    /** 取消全部订阅并释放 SSE 客户端自有资源。 */
    @Override
    public void close() {
        for (SseSubscription subscription : activeSubscriptions) {
            subscription.close();
        }
        activeSubscriptions.clear();
        if (ownsHttpClient) {
            OpenCodeOkHttpClientFactory.shutdown(httpClient);
        }
    }
}
