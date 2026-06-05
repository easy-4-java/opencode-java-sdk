package io.github.hiwepy.opencode.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.OpenCodeClientConfig;
import io.github.hiwepy.opencode.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * OpenCode Server SSE 客户端，消费 {@code GET /event} 事件流。
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

    /**
     * 异步订阅 SSE 事件流，事件通过 consumer 回调。
     *
     * @param consumer 事件消费者
     */
    public void subscribe(Consumer<Event> consumer) {
        this.running = true;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "opencode-sse");
            t.setDaemon(true);
            return t;
        });
        this.executor.submit(() -> doSubscribe(consumer));
    }

    /**
     * 阻塞式订阅，返回一个 BlockingQueue，事件入队供外部消费。
     */
    public BlockingQueue<Event> subscribeQueue() {
        BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
        subscribe(queue::offer);
        return queue;
    }

    private void doSubscribe(Consumer<Event> consumer) {
        while (running) {
            try {
                String url = config.getServerUrl() + "/event";
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
                    log.warn("SSE connection failed with status: {}, retrying in 5s", status);
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
                    log.warn("SSE connection lost, retrying in 5s", e);
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
