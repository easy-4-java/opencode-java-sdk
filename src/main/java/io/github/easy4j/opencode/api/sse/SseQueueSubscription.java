package io.github.easy4j.opencode.api.sse;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/** 同时持有事件队列和取消句柄的 SSE 队列订阅。 */
public final class SseQueueSubscription implements AutoCloseable {

    private final BlockingQueue<SseEvent> queue;
    private final SseSubscription subscription;

    public SseQueueSubscription(BlockingQueue<SseEvent> queue, SseSubscription subscription) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.subscription = Objects.requireNonNull(subscription, "subscription");
    }

    public BlockingQueue<SseEvent> getQueue() {
        return queue;
    }

    public SseSubscription getSubscription() {
        return subscription;
    }

    @Override
    public void close() {
        subscription.close();
    }
}
