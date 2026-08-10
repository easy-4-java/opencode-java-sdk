package io.github.easy4j.opencode.cli.availability;

import io.github.easy4j.opencode.cli.OpenCodeCliResult;
import lombok.Builder;
import lombok.Getter;

/**
 * Report produced by the CLI availability probe during startup or readiness checks.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeCliAvailabilityChecker
 * @see OpenCodeCliAvailabilityStatus
 */
@Getter
@Builder
public class OpenCodeCliAvailabilityReport {

    private final OpenCodeCliAvailabilityStatus status;
    private final boolean available;
    private final String configuredExecutable;
    private final String resolvedExecutablePath;
    private final String message;
    private final OpenCodeCliResult probeResult;

    /**
     * @return 是否可安全调用本地 {@code opencode}
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 构造面向日志/异常的诊断文本。
     *
     * @return 说明字符串
     */
    public String toDiagnosticMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("OpenCode CLI ");
        sb.append(available ? "ready" : "unavailable");
        sb.append(" [").append(status).append(']');
        if (configuredExecutable != null) {
            sb.append(" executable=").append(configuredExecutable);
        }
        if (resolvedExecutablePath != null) {
            sb.append(" resolved=").append(resolvedExecutablePath);
        }
        if (message != null && !message.isEmpty()) {
            sb.append(" — ").append(message);
        }
        return sb.toString();
    }
}
