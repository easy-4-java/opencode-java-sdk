package io.github.easy4j.opencode.api.sse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * OpenCode 流式聊天响应，逐段回调并在结束时返回累积全文。
 */
public class StreamingChatResponse extends CompletableFuture<String> {

    private final StringBuilder content = new StringBuilder();
    private final AtomicReference<Runnable> cancellation = new AtomicReference<>();
    private Consumer<String> deltaConsumer;

    public StreamingChatResponse onDelta(Consumer<String> consumer) {
        this.deltaConsumer = consumer;
        return this;
    }

    public void acceptDelta(String delta) {
        if (delta != null && !delta.isEmpty()) {
            content.append(delta);
            if (deltaConsumer != null) {
                deltaConsumer.accept(delta);
            }
        }
    }

    public void finish() {
        complete(content.toString());
    }

    public void fail(Throwable error) {
        completeExceptionally(error);
    }

    public String getAccumulatedContent() {
        return content.toString();
    }

    /** 绑定底层 EventSource 取消动作。 */
    public StreamingChatResponse onCancel(Runnable action) {
        cancellation.set(action);
        if (isCancelled() && action != null) {
            action.run();
        }
        return this;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        Runnable action = cancellation.getAndSet(null);
        if (action != null) {
            action.run();
        }
        return super.cancel(mayInterruptIfRunning);
    }
}
