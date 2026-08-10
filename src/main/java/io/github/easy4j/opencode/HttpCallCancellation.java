package io.github.easy4j.opencode;

/**
 * Binds a business-layer cancellation signal to a single OpenCode HTTP call.
 *
 * <p>Implementations allow callers to register a cancellation callback that will be invoked
 * when the underlying HTTP call should be aborted. The {@link #onCancel(Runnable)} method
 * returns an {@link AutoCloseable} that, when closed, unregisters the callback.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.OpenCodeHttpClient
 */
@FunctionalInterface
public interface HttpCallCancellation {

    /**
     * Registers a cancellation callback and returns a handle to unregister it.
     *
     * @param callback the runnable to invoke when cancellation is requested
     * @return an {@link AutoCloseable} that unregisters the callback when closed
     */
    AutoCloseable onCancel(Runnable callback);

    /**
     * Returns whether this cancellation has been triggered.
     *
     * @return {@code true} if cancellation has been requested, {@code false} otherwise
     */
    default boolean isCancelled() {
        return false;
    }
}
