package io.github.easy4j.opencode.api.sse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * OpenCode 流式聊天响应，逐段回调并在结束时返回累积全文。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class StreamingChatResponse extends CompletableFuture<String> {

    /**
     * 按到达顺序累计的流式文本内容。
     */
    private final StringBuilder content = new StringBuilder();
    /**
     * 底层网络调用或流式任务的取消动作。
     */
    private final AtomicReference<Runnable> cancellation = new AtomicReference<>();
    /**
     * 每次收到文本增量时调用的消费者。
     */
    private Consumer<String> deltaConsumer;

    /**
     * 注册文本增量消费者；后续到达的增量按接收顺序同步通知该消费者。
     *
     * @param consumer 事件或文本增量消费者；不得为 {@code null}
     * @return 可观察文本增量、等待完成或主动取消的流式响应
     */
    public StreamingChatResponse onDelta(Consumer<String> consumer) {
        this.deltaConsumer = consumer;
        return this;
    }

    /**
     * 追加一段文本增量并通知已注册的增量消费者。
     *
     * @param delta 本次到达的文本增量；为空时忽略
     */
    public void acceptDelta(String delta) {
        if (delta != null && !delta.isEmpty()) {
            content.append(delta);
            if (deltaConsumer != null) {
                deltaConsumer.accept(delta);
            }
        }
    }

    /**
     * 以当前累计文本正常完成流式响应。
     */
    public void finish() {
        complete(content.toString());
    }

    /**
     * 以给定异常结束流式响应。
     *
     * @param error 导致流式响应失败的异常
     */
    public void fail(Throwable error) {
        completeExceptionally(error);
    }

    /**
     * 通过 OpenCode Server HTTP API 获取累计的流式文本。
     *
     * @return 服务端或 CLI 返回的文本值；无内容时可能为空字符串
     */
    public String getAccumulatedContent() {
        return content.toString();
    }

    /**
     * 绑定底层 EventSource 取消动作。
     *
     * @param action 取消时执行的回调；不得为 {@code null}
     * @return 可观察文本增量、等待完成或主动取消的流式响应
     */
    public StreamingChatResponse onCancel(Runnable action) {
        cancellation.set(action);
        if (isCancelled() && action != null) {
            action.run();
        }
        return this;
    }

    /**
     * 取消当前异步操作或 SSE 订阅。
     *
     * @param mayInterruptIfRunning 是否允许中断正在执行的任务；底层网络调用始终会被取消
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        Runnable action = cancellation.getAndSet(null);
        if (action != null) {
            action.run();
        }
        return super.cancel(mayInterruptIfRunning);
    }
}
