package io.github.easy4j.opencode.api.model;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * OpenCode 流式聊天响应（对齐 Hermes {@code ChatStreamingResponse}）。
 * <p>
 * 继承 {@link CompletableFuture<String>}，完成时携带累积的完整文本。
 * 通过 {@link #onDelta(Consumer)} 注册增量回调。
 * </p>
 *
 * @author wandl
 * @since 2.7.x
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
