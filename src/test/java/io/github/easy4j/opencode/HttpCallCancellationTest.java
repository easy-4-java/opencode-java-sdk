package io.github.easy4j.opencode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HttpCallCancellation}.
 */
class HttpCallCancellationTest {

    @Test
    void shouldDefaultIsCancelledToFalse() {
        HttpCallCancellation cancellation = callback -> () -> {};
        assertFalse(cancellation.isCancelled());
    }

    @Test
    void shouldInvokeOnCancelCallback() {
        boolean[] invoked = {false};
        HttpCallCancellation cancellation = callback -> {
            callback.run();
            return () -> {};
        };
        cancellation.onCancel(() -> invoked[0] = true);
        assertTrue(invoked[0]);
    }

    @Test
    void shouldReturnAutoCloseableFromOnCancel() throws Exception {
        boolean[] closed = {false};
        HttpCallCancellation cancellation = callback -> () -> closed[0] = true;
        AutoCloseable handle = cancellation.onCancel(() -> {});
        assertNotNull(handle);
        handle.close();
        assertTrue(closed[0]);
    }

    @Test
    void shouldSupportCustomIsCancelled() {
        HttpCallCancellation cancellation = new HttpCallCancellation() {
            private boolean cancelled = false;
            @Override
            public AutoCloseable onCancel(Runnable callback) {
                cancelled = true;
                return () -> {};
            }
            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        };
        assertFalse(cancellation.isCancelled());
        cancellation.onCancel(() -> {});
        assertTrue(cancellation.isCancelled());
    }
}
