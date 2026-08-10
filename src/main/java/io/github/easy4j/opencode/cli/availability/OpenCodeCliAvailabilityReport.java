package io.github.easy4j.opencode.cli.availability;

import io.github.easy4j.opencode.cli.OpenCodeCliResult;
import lombok.Builder;
import lombok.Getter;

/**
 * Report produced by the CLI availability probe during startup or readiness checks.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeCliAvailabilityChecker
 * @see OpenCodeCliAvailabilityStatus
 */
@Getter
@Builder
public class OpenCodeCliAvailabilityReport {

    /**
     * 资源当前状态，具体枚举值由 OpenCode Server 定义。
     */
    private final OpenCodeCliAvailabilityStatus status;
    /**
     * 本地 OpenCode CLI 是否已通过可用性探测。
     */
    private final boolean available;
    /**
     * 配置中声明的可执行文件名称或路径。
     */
    private final String configuredExecutable;
    /**
     * 探测后解析得到的可执行文件绝对路径；未找到时为空。
     */
    private final String resolvedExecutablePath;
    /**
     * 面向诊断的可用性说明或失败原因。
     */
    private final String message;
    /**
     * 执行版本探测命令得到的退出状态和输出；命令未启动时为空。
     */
    private final OpenCodeCliResult probeResult;

    /**
     * 根据探测状态判断本地 OpenCode CLI 是否可以执行命令。
     *
     * @return 满足条件返回 {@code true}，否则返回 {@code false}
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 构造面向日志/异常的诊断文本。
     *
     * @return 服务端或 CLI 返回的文本值；无内容时可能为空字符串
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
