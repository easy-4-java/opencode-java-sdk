package io.github.easy4j.opencode.api.sse;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 可取消且幂等关闭的 OpenCode SSE 订阅句柄。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class SseSubscription implements AutoCloseable {

    /**
     * 订阅是否仍处于活动状态的原子标记。
     */
    private final AtomicBoolean active = new AtomicBoolean(true);
    /**
     * 底层网络调用或流式任务的取消动作。
     */
    private final Runnable cancellation;

    /**
     * 创建 sse subscription 实例，并按传入依赖确定资源所有权。
     *
     * @param cancellation 取消信号；为 {@code null} 时不可由外部取消
     */
    public SseSubscription(Runnable cancellation) {
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
    }

    /**
     * 取消当前异步操作或 SSE 订阅。
     *
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean cancel() {
        if (!active.compareAndSet(true, false)) {
            return false;
        }
        cancellation.run();
        return true;
    }

    /**
     * 判断当前订阅是否仍可接收事件。
     *
     * @return 满足条件返回 {@code true}，否则返回 {@code false}
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * 释放当前对象持有的连接、订阅或执行资源；重复调用是安全的。
     */
    @Override
    public void close() {
        cancel();
    }
}
