package io.github.easy4j.opencode.api.sse;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/**
 * 同时持有事件队列和取消句柄的 SSE 队列订阅。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class SseQueueSubscription implements AutoCloseable {

    /**
     * 存放最新 SSE 事件的有界队列；容量耗尽时淘汰最旧事件。
     */
    private final BlockingQueue<SseEvent> queue;
    /**
     * 控制底层 SSE 连接生命周期的订阅句柄。
     */
    private final SseSubscription subscription;

    /**
     * 创建 sse queue subscription 实例，并按传入依赖确定资源所有权。
     *
     * @param queue 由调用方消费的有界 SSE 事件队列
     * @param subscription 控制底层 SSE 连接生命周期的订阅句柄
     */
    public SseQueueSubscription(BlockingQueue<SseEvent> queue, SseSubscription subscription) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.subscription = Objects.requireNonNull(subscription, "subscription");
    }

    /**
     * 通过 OpenCode Server HTTP API 获取SSE 事件队列。
     *
     * @return OpenCode SDK 返回的SSE 事件队列对象
     */
    public BlockingQueue<SseEvent> getQueue() {
        return queue;
    }

    /**
     * 通过 OpenCode Server HTTP API 获取SSE 订阅句柄。
     *
     * @return 用于取消连接并释放资源的订阅句柄
     */
    public SseSubscription getSubscription() {
        return subscription;
    }

    /**
     * 释放当前对象持有的连接、订阅或执行资源；重复调用是安全的。
     */
    @Override
    public void close() {
        subscription.close();
    }
}
