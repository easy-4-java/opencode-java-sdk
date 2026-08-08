package io.github.easy4j.opencode.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.HttpCallCancellation;
import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.api.mapper.ChatMessageMapper;
import io.github.easy4j.opencode.api.model.ChatRequest;
import io.github.easy4j.opencode.api.model.ChatResponse;
import io.github.easy4j.opencode.api.model.ChatStreamingResponse;
import io.github.easy4j.opencode.api.model.Event;
import io.github.easy4j.opencode.api.model.PromptRequest;
import io.github.easy4j.opencode.api.model.PromptResult;
import okhttp3.OkHttpClient;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * OpenCode 聊天场景客户端，统一提供完整响应与流式响应。
 *
 * <p>普通 Prompt/Chat 调用继承自 {@link OpenCodeHttpClient}；事件订阅是流式聊天的
 * 内部实现细节，业务调用方通过本类即可完成两种响应模式。</p>
 */
public class OpenCodeChatClient extends OpenCodeHttpClient {

    private final OpenCodeHttpClientConfig config;
    private final OpenCodeSseClient eventClient;
    private final ExecutorService streamExecutor;

    public OpenCodeChatClient(OpenCodeHttpClientConfig config) {
        this(config, new ObjectMapper(), null);
    }

    public OpenCodeChatClient(OpenCodeHttpClientConfig config, ObjectMapper objectMapper,
                              OkHttpClient httpClient) {
        super(config, objectMapper, httpClient);
        this.config = Objects.requireNonNull(config, "config");
        this.eventClient = new OpenCodeSseClient(config, objectMapper, getOkHttpClient());
        this.streamExecutor = createStreamExecutor(config);
    }

    private static ExecutorService createStreamExecutor(OpenCodeHttpClientConfig config) {
        int corePoolSize = Math.max(1, config.getStreamCorePoolSize());
        int maxPoolSize = Math.max(corePoolSize, config.getStreamMaxPoolSize());
        AtomicInteger threadIndex = new AtomicInteger();
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                Math.max(1L, config.getStreamKeepAliveMillis()), TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(Math.max(1, config.getStreamQueueCapacity())), runnable -> {
                    Thread thread = new Thread(runnable,
                            "opencode-stream-consumer-" + threadIndex.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public ChatResponse chatCompletion(String sessionId, ChatRequest request) {
        PromptResult result = prompt(sessionId, ChatMessageMapper.toPromptRequest(request));
        return ChatMessageMapper.toChatResponse(result);
    }

    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey) {
        PromptResult result = chatCompletionWithSession(ChatMessageMapper.toPromptRequest(request), sessionKey);
        return ChatMessageMapper.toChatResponse(result);
    }

    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        PromptResult result = chatCompletionWithSession(
                ChatMessageMapper.toPromptRequest(request), sessionKey, cancellation);
        return ChatMessageMapper.toChatResponse(result);
    }

    public ChatStreamingResponse chatCompletionStream(ChatRequest request, String sessionKey) {
        return chatCompletionStream(request, sessionKey, null, null);
    }

    public ChatStreamingResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context) {
        return chatCompletionStream(request, sessionKey, context, null);
    }

    /** 在事件订阅启动前绑定增量回调，避免丢失首批分片。 */
    public ChatStreamingResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context,
                                                       Consumer<String> deltaConsumer) {
        String sessionId = ensureSession(sessionKey, context);
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        ChatStreamingResponse stream = new ChatStreamingResponse().onDelta(deltaConsumer);
        OpenCodeSseClient.QueueSubscription subscription = eventClient.subscribeQueueSubscription(context);
        BlockingQueue<Event> queue = subscription.getQueue();

        try {
            streamExecutor.submit(() -> consumeEvents(sessionId, queue, subscription, stream));
        } catch (RejectedExecutionException error) {
            subscription.close();
            stream.fail(new IllegalStateException("OpenCode stream executor is full", error));
            return stream;
        }

        try {
            if (!promptAsync(sessionId, promptRequest, context)) {
                subscription.close();
                stream.fail(new IllegalStateException("OpenCode async prompt was rejected"));
            }
        } catch (RuntimeException error) {
            subscription.close();
            stream.fail(error);
        }
        return stream;
    }

    private void consumeEvents(String sessionId, BlockingQueue<Event> queue,
                               OpenCodeSseClient.QueueSubscription subscription,
                               ChatStreamingResponse stream) {
        try {
            long timeoutMillis = Math.max(1L, config.getReadTimeoutMillis());
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (!stream.isDone() && System.currentTimeMillis() < deadline) {
                Event event = queue.poll(3, TimeUnit.SECONDS);
                if (Objects.isNull(event) || !matchesSession(event, sessionId)) {
                    continue;
                }
                String type = event.getType();
                if (Objects.isNull(type)) {
                    continue;
                }
                if (type.contains("text.delta") || type.contains("message.part.updated")) {
                    stream.acceptDelta(extractDeltaText(event));
                }
                if (type.contains("session.status") || type.contains("session.idle")) {
                    String status = Objects.toString(event.getProperties().get("status"), null);
                    if (Objects.equals("idle", status) || type.contains("idle")) {
                        stream.finish();
                        return;
                    }
                }
                if (type.contains("session.error")) {
                    stream.fail(new IllegalStateException(
                            Objects.toString(event.getProperties().get("error"), "unknown error")));
                    return;
                }
            }
            if (!stream.isDone()) {
                stream.fail(new IllegalStateException("Stream timed out for session: " + sessionId));
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            stream.fail(error);
        } catch (RuntimeException error) {
            stream.fail(error);
        } finally {
            subscription.close();
        }
    }

    private boolean matchesSession(Event event, String sessionId) {
        return Objects.nonNull(event.getProperties())
                && Objects.equals(sessionId, Objects.toString(event.getProperties().get("sessionID"), null));
    }

    private String extractDeltaText(Event event) {
        if (Objects.isNull(event.getProperties())) {
            return null;
        }
        Object part = event.getProperties().get("part");
        if (part instanceof Map) {
            Object text = ((Map<?, ?>) part).get("text");
            if (Objects.nonNull(text)) {
                return text.toString();
            }
        }
        return Objects.toString(event.getProperties().get("delta"), null);
    }

    /** 原始事件客户端，仅供非聊天事件等高级场景使用。 */
    public OpenCodeSseClient events() {
        return eventClient;
    }

    @Override
    public void close() {
        streamExecutor.shutdownNow();
        eventClient.close();
        super.close();
    }
}
