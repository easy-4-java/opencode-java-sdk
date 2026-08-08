package io.github.easy4j.opencode.api.model;

import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

/**
 * OpenCode 流式聊天响应（对齐 Hermes {@code ChatStreamingResponse}）。
 * <p>
 * 继承 {@link CompletableFuture<String>}，完成时携带累积的完整文本。
 * 通过 {@link #onDelta(Consumer)} 注册增量回调。
 * </p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 2.7.x
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
