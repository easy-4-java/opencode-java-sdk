package io.github.easy4j.opencode;

/**
 * Binds a business-layer cancellation signal to a single OpenCode HTTP call.
 * <p>Implementations allow callers to register a cancellation callback that will be invoked
 * when the underlying HTTP call should be aborted. The {@link #onCancel(Runnable)} method
 * returns an {@link AutoCloseable} that, when closed, unregisters the callback.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient
 */
@FunctionalInterface
public interface HttpCallCancellation {

    /**
     * Registers a cancellation callback and returns a handle to unregister it.
     *
     * @param callback the runnable to invoke when cancellation is requested
     * @return OpenCode SDK 返回的回调注销句柄对象
     */
    AutoCloseable onCancel(Runnable callback);

    /**
     * Returns whether this cancellation has been triggered.
     *
     * @return 满足条件返回 {@code true}，否则返回 {@code false}
     */
    default boolean isCancelled() {
        return false;
    }
}
