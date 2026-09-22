package io.github.easy4j.opencode.cli;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle for an asynchronously streaming OpenCode CLI child process.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
public final class OpenCodeCliStreamHandle {

    private final Process process;
    private final CompletableFuture<OpenCodeCliResult> completion;
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();

    OpenCodeCliStreamHandle(Process process, CompletableFuture<OpenCodeCliResult> completion) {
        this.process = process;
        this.completion = completion;
    }

    public CompletableFuture<OpenCodeCliResult> getCompletion() {
        return completion;
    }

    public boolean isRunning() {
        return process != null && process.isAlive() && !completion.isDone();
    }

    /**
     * Cancel the local child process tree. This does not call the OpenCode HTTP
     * session abort endpoint.
     *
     * @return true when cancellation was requested before completion
     */
    public boolean cancel() {
        if (completion.isDone()) {
            return false;
        }
        cancellationRequested.set(true);
        terminateProcessTree();
        return true;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested.get();
    }

    private void terminateProcessTree() {
        if (process == null) {
            return;
        }
        // JDK 8 没有 ProcessHandle API——只能销毁直接子进程，不能杀进程树。
        // 在 Java 9+ 上可通过 process.toHandle().descendants() 扩展进程树清理。
        try {
            process.destroy();
        } catch (RuntimeException ignored) {
            // best effort
        }
        if (process.isAlive()) {
            try {
                process.destroyForcibly();
            } catch (RuntimeException ignored) {
                // best effort
            }
        }
    }
}
