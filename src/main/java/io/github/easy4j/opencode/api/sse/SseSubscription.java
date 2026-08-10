package io.github.easy4j.opencode.api.sse;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** 可取消且幂等关闭的 OpenCode SSE 订阅句柄。 */
public final class SseSubscription implements AutoCloseable {

    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Runnable cancellation;

    public SseSubscription(Runnable cancellation) {
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
    }

    public boolean cancel() {
        if (!active.compareAndSet(true, false)) {
            return false;
        }
        cancellation.run();
        return true;
    }

    public boolean isActive() {
        return active.get();
    }

    @Override
    public void close() {
        cancel();
    }
}
