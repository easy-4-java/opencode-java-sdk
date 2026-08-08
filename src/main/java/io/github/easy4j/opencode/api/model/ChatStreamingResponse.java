package io.github.easy4j.opencode.api.model;

import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

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
@Deprecated
public class ChatStreamingResponse extends io.github.easy4j.opencode.api.sse.StreamingChatResponse {

    /**
     * 注册增量文本回调，每收到一段 delta 触发一次。
     */
    public ChatStreamingResponse onDelta(Consumer<String> consumer) {
        super.onDelta(consumer);
        return this;
    }
}
