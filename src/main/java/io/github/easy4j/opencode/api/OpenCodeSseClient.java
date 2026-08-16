package io.github.easy4j.opencode.api;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
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
import okhttp3.extension.logging.HttpLogLevel;

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
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Slf4j
public class OpenCodeSseClient implements AutoCloseable {

    /**
     * 当前客户端使用的不可变配置引用。
     */
    private final OpenCodeHttpClientConfig config;
    /**
     * SSE 事件 JSON 的反序列化映射器。
     */
    private final ObjectMapper mapper;
    /**
     * 执行连接复用和异步网络请求的 OkHttp 客户端。
     */
    private final OkHttpClient httpClient;
    /**
     * 是否由当前 SSE 客户端创建并负责关闭底层 OkHttp 资源。
     */
    private final boolean ownsHttpClient;
    /**
     * 当前仍处于活动状态的订阅集合，用于统一关闭和资源回收。
     */
    private final Set<SseSubscription> activeSubscriptions = ConcurrentHashMap.newKeySet();

    /**
     * 使用 SDK 自建 OkHttpClient 创建 SSE 客户端。
     *
     * @param config 客户端配置；不得为 {@code null}
     */
    public OpenCodeSseClient(OpenCodeHttpClientConfig config) {
        this(config, new JsonMapper(), null);
    }

    /**
     * 使用共享 ObjectMapper 和 OkHttpClient 创建 SSE 客户端。
     *
     * @param config 客户端配置；不得为 {@code null}
     * @param objectMapper JSON 映射器；为 {@code null} 时使用 SDK 默认配置
     * @param httpClient 可复用的 OkHttp 客户端；为 {@code null} 时由 SDK 创建
     */
    public OpenCodeSseClient(OpenCodeHttpClientConfig config, ObjectMapper objectMapper,
                             OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.mapper = Objects.isNull(objectMapper) ? JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build() : objectMapper;
        this.ownsHttpClient = Objects.isNull(httpClient);
        OkHttpClient baseClient = ownsHttpClient
                ? OpenCodeOkHttpClientFactory.create(config) : httpClient;
        this.httpClient = baseClient.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build();
        debug(HttpLogLevel.BASIC, "OpenCode SSE client initialized: baseUrl={}, maxRequests={}, "
                        + "maxRequestsPerHost={}, eventQueueCapacity={}, reconnectPolicy=none, debugLevel={}",
                config.getBaseUrl(), config.getMaxRequests(), config.getMaxRequestsPerHost(),
                config.getStreamEventQueueCapacity(), config.getDebug().getLevel());
    }

    /**
     * 订阅全局事件流。
     *
     * @param consumer 事件或文本增量消费者；不得为 {@code null}
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription subscribeEvents(Consumer<SseEvent> consumer) {
        return subscribeEvents(consumer, null);
    }

    /**
     * 使用请求上下文订阅全局事件流。
     *
     * @param consumer 事件或文本增量消费者；不得为 {@code null}
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription subscribeEvents(Consumer<SseEvent> consumer,
                                           OpenCodeRequestContext context) {
        Objects.requireNonNull(consumer, "consumer");
        Request request = buildRequest(context);

        // EventSource 创建与调用方取消可能并发发生。两个原子引用既支持关闭已建立连接，
        // 也支持在连接稍后创建完成时通过下方 active 检查补偿取消。
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
                debug(HttpLogLevel.BASIC, "OpenCode SSE connected: streamType=events, url={}, status={}, elapsedMs={}",
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
                    if (config.getDebug().allows(HttpLogLevel.BODY)) {
                        log.debug("Failed to parse OpenCode SSE event: data={}", truncate(data), error);
                    } else {
                        debug(HttpLogLevel.BASIC, "Failed to parse OpenCode SSE event: dataLength={}, error={}",
                                data.length(), error.getMessage());
                    }
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                closeSubscription(subscriptionRef);
                debug(HttpLogLevel.BASIC, "OpenCode SSE closed: streamType=events, url={}", request.url());
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

        // 处理“订阅先关闭、EventSource 后返回”的竞态，防止失去引用的连接继续接收事件。
        if (!subscription.isActive()) {
            eventSource.cancel();
        }
        return subscription;
    }

    /**
     * 创建带有界队列的全局事件订阅。
     *
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @return 包含有界事件队列和取消句柄的订阅对象
     */
    public SseQueueSubscription subscribeEventsQueue(OpenCodeRequestContext context) {
        BlockingQueue<SseEvent> queue = new ArrayBlockingQueue<>(
                Math.max(1, config.getStreamEventQueueCapacity()));
        SseSubscription subscription = subscribeEvents(event -> offerLatest(queue, event), context);
        return new SseQueueSubscription(queue, subscription);
    }

    /**
     * 订阅指定 session 的事件。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param consumer 事件或文本增量消费者；不得为 {@code null}
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription subscribeSessionEvents(String sessionId,
                                                  Consumer<SseEvent> consumer) {
        return subscribeSessionEvents(sessionId, consumer, null);
    }

    /**
     * 使用请求上下文订阅指定 session 的事件。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param consumer 事件或文本增量消费者；不得为 {@code null}
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription subscribeSessionEvents(String sessionId,
                                                  Consumer<SseEvent> consumer,
                                                  OpenCodeRequestContext context) {
        return subscribeEvents(filterBySession(sessionId, consumer), context);
    }

    /**
     * 使用类型化处理器订阅指定 session 的事件。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param handler 类型化事件处理器；不得为 {@code null}
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription subscribeSessionEvents(String sessionId, EventHandler handler) {
        return subscribeSessionEvents(sessionId, handler, null);
    }

    /**
     * 使用请求上下文和类型化处理器订阅指定 session 的事件。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param handler 类型化事件处理器；不得为 {@code null}
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription subscribeSessionEvents(String sessionId, EventHandler handler,
                                                  OpenCodeRequestContext context) {
        Objects.requireNonNull(handler, "handler");
        return subscribeEvents(filterBySession(sessionId, handler::onEvent), context);
    }

    /**
     * 订阅指定类型集合中的事件。
     *
     * @param types 允许通过的事件类型集合；为空时不过滤
     * @param consumer 事件或文本增量消费者；不得为 {@code null}
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription subscribeEventTypes(Set<String> types, Consumer<SseEvent> consumer) {
        return subscribeEventTypes(types, consumer, null);
    }

    /**
     * 使用请求上下文订阅指定类型集合中的事件。
     *
     * @param types 允许通过的事件类型集合；为空时不过滤
     * @param consumer 事件或文本增量消费者；不得为 {@code null}
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription subscribeEventTypes(Set<String> types, Consumer<SseEvent> consumer,
                                               OpenCodeRequestContext context) {
        return subscribeEvents(filterByTypes(types, consumer), context);
    }

    /**
     * 返回当前活动订阅数量。
     *
     * @return 按接口语义计算的数量或状态数值
     */
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
            // 队列订阅是显式兼容接口；消费者落后时淘汰最旧事件，保证 I/O 回调永不阻塞。
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

    private void debug(HttpLogLevel level, String message, Object... arguments) {
        if (config.getDebug().allows(level)) {
            log.debug(message, arguments);
        }
    }

    private String truncate(String value) {
        int limit = config.getDebug().resolveMaxContentLength();
        return value.length() <= limit ? value : value.substring(0, limit) + "...<truncated>";
    }

    /**
     * 取消全部订阅并释放 SSE 客户端自有资源。
     */
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
