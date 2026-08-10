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
 * <p>普通 Prompt/Chat 调用继承自 {@link OpenCodeHttpClient}；事件订阅是流式聊天的
 * 内部实现细节，业务调用方通过本类即可完成两种响应模式。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class OpenCodeChatClient extends OpenCodeHttpClient {

    /**
     * 当前客户端使用的不可变配置引用。
     */
    private final OpenCodeHttpClientConfig config;
    /**
     * 当前组件复用的SSE 客户端；资源所有权由对应 owns 字段决定。
     */
    private final OpenCodeSseClient sseClient;
    /**
     * 是否由当前聊天客户端创建并负责关闭 SSE 客户端。
     */
    private final boolean ownsSseClient;
    /**
     * 为流式聊天设置完成超时的单线程调度器。
     */
    private final ScheduledExecutorService timeoutScheduler;

    /**
     * 创建 open code chat client 实例，并按传入依赖确定资源所有权。
     *
     * @param config 客户端配置；不得为 {@code null}
     */
    public OpenCodeChatClient(OpenCodeHttpClientConfig config) {
        this(config, new ObjectMapper(), null);
    }

    /**
     * 创建 open code chat client 实例，并按传入依赖确定资源所有权。
     *
     * @param config 客户端配置；不得为 {@code null}
     * @param objectMapper JSON 映射器；为 {@code null} 时使用 SDK 默认配置
     * @param httpClient 可复用的 OkHttp 客户端；为 {@code null} 时由 SDK 创建
     */
    public OpenCodeChatClient(OpenCodeHttpClientConfig config, ObjectMapper objectMapper,
                              OkHttpClient httpClient) {
        super(config, objectMapper, httpClient);
        this.config = Objects.requireNonNull(config, "config");
        this.sseClient = new OpenCodeSseClient(config, getObjectMapper(), getOkHttpClient());
        this.ownsSseClient = true;
        this.timeoutScheduler = createTimeoutScheduler();
    }

    /**
     * 创建 open code chat client 实例，并按传入依赖确定资源所有权。
     *
     * @param config 客户端配置；不得为 {@code null}
     * @param objectMapper JSON 映射器；为 {@code null} 时使用 SDK 默认配置
     * @param httpClient 可复用的 OkHttp 客户端；为 {@code null} 时由 SDK 创建
     * @param sseClient SSE 客户端；为 {@code null} 时按配置创建
     */
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

    /**
     * 同步提交聊天请求并返回完整响应。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param request 请求对象；不得为 {@code null}
     * @return OpenCode SDK 返回的聊天响应对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ChatResponse chatCompletion(String sessionId, ChatRequest request) {
        return chatCompletionAsync(sessionId, request).join();
    }

    /**
     * 异步完成指定会话的聊天请求。
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param request 请求对象；不得为 {@code null}
     * @return 异步结果；失败时以异常方式完成
     */
    public CompletableFuture<ChatResponse> chatCompletionAsync(String sessionId, ChatRequest request) {
        return promptCompletionAsync(sessionId, ChatMessageMapper.toPromptRequest(request), null)
                .thenApply(ChatMessageMapper::toChatResponse);
    }

    /**
     * 复用或创建稳定会话后提交聊天请求并返回结果。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return OpenCode SDK 返回的聊天响应对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionWithSessionAsync(request, sessionKey, null).join();
    }

    /**
     * 复用或创建稳定会话后提交聊天请求并返回结果。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param cancellation 取消信号；为 {@code null} 时不可由外部取消
     * @return OpenCode SDK 返回的聊天响应对象
     * @throws io.github.easy4j.opencode.exception.OpenCodeHttpException HTTP 请求失败或响应无法解析
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        return chatCompletionWithSessionAsync(request, sessionKey, cancellation).join();
    }

    /**
     * 异步查找会话并完成聊天请求。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param cancellation 取消信号；为 {@code null} 时不可由外部取消
     * @return 异步结果；失败时以异常方式完成
     */
    public CompletableFuture<ChatResponse> chatCompletionWithSessionAsync(ChatRequest request, String sessionKey,
                                                                         HttpCallCancellation cancellation) {
        return ensureSessionAsync(sessionKey, null, cancellation)
                .thenCompose(sessionId -> promptCompletionAsync(sessionId,
                        ChatMessageMapper.toPromptRequest(request), cancellation))
                .thenApply(ChatMessageMapper::toChatResponse);
    }

    /**
     * 创建流式聊天响应，异步订阅会话事件并持续交付文本增量。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @return 可观察文本增量、等待完成或主动取消的流式响应
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey) {
        return chatCompletionStream(request, sessionKey, null, null);
    }

    /**
     * 创建流式聊天响应，异步订阅会话事件并持续交付文本增量。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @return 可观察文本增量、等待完成或主动取消的流式响应
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context) {
        return chatCompletionStream(request, sessionKey, context, null);
    }

    /**
     * 在事件订阅启动前绑定增量回调，避免丢失首批分片。
     *
     * @param request 请求对象；不得为 {@code null}
     * @param sessionKey 用于稳定复用会话的业务键；相同键映射到同一标题
     * @param context 请求上下文；为 {@code null} 时不附加目录头
     * @param deltaConsumer 文本增量消费者；不得为 {@code null}
     * @return 可观察文本增量、等待完成或主动取消的流式响应
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request, String sessionKey,
                                                       OpenCodeRequestContext context,
                                                       Consumer<String> deltaConsumer) {
        PromptRequest promptRequest = ChatMessageMapper.toPromptRequest(request);
        StreamingChatResponse stream = new StreamingChatResponse().onDelta(deltaConsumer);

        // 先解析稳定会话，再建立 SSE 订阅，最后发送 prompt_async。
        // 该顺序保证服务端产生首个文本分片前监听器已经就绪，避免丢失开头内容。
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

            // 正常完成、异常完成和主动取消共用同一个幂等清理动作，避免遗留 SSE 连接或超时任务。
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

    /**
     * 释放当前对象持有的连接、订阅或执行资源；重复调用是安全的。
     */
    @Override
    public void close() {
        timeoutScheduler.shutdownNow();
        if (ownsSseClient) {
            sseClient.close();
        }
        super.close();
    }
}
