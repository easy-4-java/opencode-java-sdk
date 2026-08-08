package io.github.easy4j.opencode;

/** 将业务层取消信号绑定到一次 OpenCode HTTP 调用。 */
@FunctionalInterface
public interface HttpCallCancellation {

    AutoCloseable onCancel(Runnable callback);

    default boolean isCancelled() {
        return false;
    }
}
