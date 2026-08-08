package io.github.easy4j.opencode.api.model;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Streaming chat response for OpenCode (aligned with Hermes {@code ChatStreamingResponse}).
 *
 * <p>Extends {@link CompletableFuture CompletableFuture&lt;String&gt;} which, upon completion,
 * carries the accumulated full text. Register incremental callbacks via {@link #onDelta(Consumer)}.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see ChatRequest
 * @see ChatResponse
 */
public class ChatStreamingResponse extends CompletableFuture<String> {

    private final StringBuilder content = new StringBuilder();
    private Consumer<String> deltaConsumer;

    /**
     * 注册增量文本回调，每收到一段 delta 触发一次。
     */
    public ChatStreamingResponse onDelta(Consumer<String> consumer) {
        this.deltaConsumer = consumer;
        return this;
    }

    /**
     * 接收一段增量文本。
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
     * 流结束，完成 future。
     */
    public void finish() {
        complete(content.toString());
    }

    /**
     * 流异常，异常完成 future。
     */
    public void fail(Throwable error) {
        completeExceptionally(error);
    }

    /**
     * 获取已累积的完整文本。
     */
    public String getAccumulatedContent() {
        return content.toString();
    }
}
