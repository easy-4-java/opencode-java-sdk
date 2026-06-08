package io.github.hiwepy.opencode.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.OpenCodeClientConfig;
import io.github.hiwepy.opencode.model.Event;
import io.github.hiwepy.opencode.model.event.EventParser;
import io.github.hiwepy.opencode.model.event.TypedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * OpenCode Server SSE 客户端，消费 {@code GET /event}、{@code /global/event}、{@code /api/event} 事件流。
 * <p>
 * 支持泛化 {@link Event} 回调和类型化 {@link TypedEvent} 回调。
 * </p>
 */
public class OpenCodeSseClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeSseClient.class);

    private final OpenCodeClientConfig config;
    private final ObjectMapper mapper;
    private volatile boolean running;
    private HttpURLConnection connection;
    private ExecutorService executor;

    public OpenCodeSseClient(OpenCodeClientConfig config) {
        this.config = config;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ============================================================
    // V1 事件流（/event）
    // ============================================================

    /**
     * 异步订阅 SSE 事件流，事件通过 consumer 回调。
     */
    public void subscribe(Consumer<Event> consumer) {
        subscribeToPath("/event", consumer);
    }

    /**
     * 阻塞式订阅，返回一个 BlockingQueue，事件入队供外部消费。
     */
    public BlockingQueue<Event> subscribeQueue() {
        BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
        subscribe(queue::offer);
        return queue;
    }

    // ============================================================
    // 全局事件流（/global/event）
    // ============================================================

    /**
     * 异步订阅全局 SSE 事件流。
     */
    public void subscribeGlobal(Consumer<Event> consumer) {
        subscribeToPath("/global/event", consumer);
    }

    public BlockingQueue<Event> subscribeGlobalQueue() {
        BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
        subscribeGlobal(queue::offer);
        return queue;
    }

    // ============================================================
    // V2 事件流（/api/event）
    // ============================================================

    /**
     * 异步订阅 V2 API SSE 事件流。
     */
    public void subscribeV2(Consumer<Event> consumer) {
        subscribeToPath("/api/event", consumer);
    }

    public BlockingQueue<Event> subscribeV2Queue() {
        BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
        subscribeV2(queue::offer);
        return queue;
    }

    // ============================================================
    // 类型化事件消费
    // ============================================================

    /**
     * 异步订阅并自动转换为 {@link TypedEvent}。
     */
    public void subscribeTyped(Consumer<TypedEvent> consumer) {
        subscribe(event -> consumer.accept(EventParser.parse(event)));
    }

    /**
     * 异步订阅并按事件类型分发到不同 handler。
     */
    public void subscribeTyped(TypedEventHandler handler) {
        subscribe(event -> {
            TypedEvent typed = EventParser.parse(event);
            String type = typed.getType();
            if (type == null) return;

            if (type.contains(".text.delta")) {
                handler.onTextDelta(typed);
            } else if (type.contains(".tool.")) {
                handler.onTool(typed);
            } else if (type.contains(".step.")) {
                handler.onStep(typed);
            } else if (type.contains(".reasoning.delta")) {
                handler.onReasoningDelta(typed);
            } else if (type.contains(".shell.")) {
                handler.onShell(typed);
            }
            handler.onAny(typed);
        });
    }

    /**
     * 类型化事件多路分发接口。
     */
    public interface TypedEventHandler {
        default void onTextDelta(TypedEvent event) {}
        default void onTool(TypedEvent event) {}
        default void onStep(TypedEvent event) {}
        default void onReasoningDelta(TypedEvent event) {}
        default void onShell(TypedEvent event) {}
        default void onAny(TypedEvent event) {}
    }

    // ============================================================
    // 内部实现
    // ============================================================

    private void subscribeToPath(String path, Consumer<Event> consumer) {
        this.running = true;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "opencode-sse");
            t.setDaemon(true);
            return t;
        });
        this.executor.submit(() -> doSubscribe(path, consumer));
    }

    private void doSubscribe(String path, Consumer<Event> consumer) {
        while (running) {
            try {
                String url = config.getServerUrl() + path;
                connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(config.getConnectTimeoutMillis());
                connection.setReadTimeout(0); // SSE 无读超时
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Cache-Control", "no-cache");

                String password = config.resolvePassword();
                if (!password.isEmpty()) {
                    String credentials = Base64.getEncoder()
                            .encodeToString((config.getUsername() + ":" + password).getBytes());
                    connection.setRequestProperty("Authorization", "Basic " + credentials);
                }

                int status = connection.getResponseCode();
                if (status != 200) {
                    log.warn("SSE connection to {} failed with status: {}, retrying in 5s", path, status);
                    Thread.sleep(5000);
                    continue;
                }

                log.info("SSE connected to {}", url);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String json = line.substring(6).trim();
                            if (!json.isEmpty()) {
                                try {
                                    Event event = mapper.readValue(json, Event.class);
                                    consumer.accept(event);
                                } catch (Exception e) {
                                    log.debug("Failed to parse SSE event: {}", json, e);
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (running) {
                    log.warn("SSE connection lost for {}, retrying in 5s", path, e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    /**
     * 停止事件流订阅。
     */
    public void stop() {
        this.running = false;
        if (connection != null) {
            connection.disconnect();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void close() {
        stop();
    }
}
