package io.github.easy4j.opencode.api.sse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * OpenCode 流式聊天响应，逐段回调并在结束时返回累积全文。
 */
public class StreamingChatResponse extends CompletableFuture<String> {

    private final StringBuilder content = new StringBuilder();
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
}
