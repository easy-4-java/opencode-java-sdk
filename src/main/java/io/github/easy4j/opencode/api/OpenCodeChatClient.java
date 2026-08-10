package io.github.easy4j.opencode.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.HttpCallCancellation;
import io.github.easy4j.opencode.OpenCodeHttpClientConfig;
import io.github.easy4j.opencode.api.mapper.ChatMessageMapper;
import io.github.easy4j.opencode.api.model.ChatRequest;
import io.github.easy4j.opencode.api.model.ChatResponse;
import io.github.easy4j.opencode.api.sse.SseEvent;
import io.github.easy4j.opencode.api.sse.SseSubscription;
import io.github.easy4j.opencode.api.sse.StreamingChatResponse;
import io.github.easy4j.opencode.api.model.PromptRequest;
import io.github.easy4j.opencode.api.model.PromptResult;
import okhttp3.OkHttpClient;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
    private final OpenCodeSseClient sseClient;
    private final boolean ownsSseClient;
    private final ScheduledExecutorService timeoutScheduler;

    public OpenCodeChatClient(OpenCodeHttpClientConfig config) {
        this(config, new ObjectMapper(), null);
    }

    public OpenCodeChatClient(OpenCodeHttpClientConfig config, ObjectMapper objectMapper,
                              OkHttpClient httpClient) {
        super(config, objectMapper, httpClient);
        this.config = Objects.requireNonNull(config, "config");
        this.sseClient = new OpenCodeSseClient(config, getObjectMapper(), getOkHttpClient());
        this.ownsSseClient = true;
        this.timeoutScheduler = createTimeoutScheduler();
    }

    public OpenCodeChatClient(OpenCodeHttpClientConfig config, ObjectMapper objectMapper,
                              OkHttpClient httpClient, OpenCodeSseClient sseClient) {
        super(config, objectMapper, httpClient);
        this.config = Objects.requireNonNull(config, "config");
        this.sseClient = Objects.requireNonNull(sseClient, "sseClient");
        this.ownsSseClient = false;
        this.timeoutScheduler = createTimeoutScheduler();
    }

    private static ScheduledExecutorService createTimeoutScheduler() {
        AtomicInteger threadIndex = new AtomicInteger();
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable,
                            "opencode-stream-timeout-" + threadIndex.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
    }

    public ChatResponse chatCompletion(String sessionId, ChatRequest request) {
        return chatCompletionAsync(sessionId, request).join();
    }

    /** 异步完成指定会话的聊天请求。 */
    public CompletableFuture<ChatResponse> chatCompletionAsync(String sessionId, ChatRequest request) {
        return promptCompletionAsync(sessionId, ChatMessageMapper.toPromptRequest(request), null)
                .thenApply(ChatMessageMapper::toChatResponse);
    }

    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionWithSessionAsync(request, sessionKey, null).join();
    }

    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        return chatCompletionWithSessionAsync(request, sessionKey, cancellation).join();
    }

    /** 异步查找会话并完成聊天请求。 */
    public CompletableFuture<ChatResponse> chatCompletionWithSessionAsync(ChatRequest request, String sessionKey,
                                                                         HttpCallCancellation cancellation) {
        return ensureSessionAsync(sessionKey, null, cancellation)
                .thenCompose(sessionId -> promptCompletionAsync(sessionId,
                        ChatMessageMapper.toPromptRequest(request), cancellation))
                .thenApply(ChatMessageMapper::toChatResponse);
    }

    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey) {
        return chatCompletionStream(request, sessionKey, null, null);
    }

    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context) {
        return chatCompletionStream(request, sessionKey, context, null);
    }

    /** 在事件订阅启动前绑定增量回调，避免丢失首批分片。 */
    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context,
                                                       Consumer<String> deltaConsumer) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        StreamingChatResponse stream = new StreamingChatResponse().onDelta(deltaConsumer);
        ensureSessionAsync(sessionKey, context, null).whenComplete((sessionId, sessionError) -> {
            if (Objects.nonNull(sessionError)) {
                stream.fail(sessionError);
                return;
            }
            SseSubscription subscription = sseClient.subscribeSessionEvents(sessionId,
                    event -> handleEvent(sessionId, event, stream), context);
            ScheduledFuture<?> timeout = timeoutScheduler.schedule(() -> {
                if (!stream.isDone()) {
                    stream.fail(new IllegalStateException("Stream timed out for session: " + sessionId));
                }
            }, Math.max(1L, config.getReadTimeoutMillis()), TimeUnit.MILLISECONDS);
            Runnable close = () -> {
                timeout.cancel(false);
                subscription.cancel();
            };
            stream.onCancel(close);
            stream.whenComplete((value, error) -> close.run());
            promptAsync(sessionId, promptRequest, context).whenComplete((accepted, promptError) -> {
                if (Objects.nonNull(promptError)) {
                    stream.fail(promptError);
                } else if (!Boolean.TRUE.equals(accepted)) {
                    stream.fail(new IllegalStateException("OpenCode async prompt was rejected"));
                }
            });
        });
        return stream;
    }

    private void handleEvent(String sessionId, SseEvent event, StreamingChatResponse stream) {
        if (Objects.isNull(event) || !matchesSession(event, sessionId) || stream.isDone()) {
            return;
        }
        String type = event.getType();
        if (Objects.isNull(type)) {
            return;
        }
        if (type.contains("text.delta") || type.contains("message.part.updated")) {
            stream.acceptDelta(extractDeltaText(event));
        }
        if (type.contains("session.status") || type.contains("session.idle")) {
            String status = Objects.toString(event.getProperties().get("status"), null);
            if (Objects.equals("idle", status) || type.contains("idle")) {
                stream.finish();
            }
        }
        if (type.contains("session.error")) {
            stream.fail(new IllegalStateException(
                    Objects.toString(event.getProperties().get("error"), "unknown error")));
        }
    }

    private boolean matchesSession(SseEvent event, String sessionId) {
        return Objects.nonNull(event.getProperties())
                && Objects.equals(sessionId, Objects.toString(event.getProperties().get("sessionID"), null));
    }

    private String extractDeltaText(SseEvent event) {
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

    @Override
    public void close() {
        timeoutScheduler.shutdownNow();
        if (ownsSseClient) {
            sseClient.close();
        }
        super.close();
    }
}
