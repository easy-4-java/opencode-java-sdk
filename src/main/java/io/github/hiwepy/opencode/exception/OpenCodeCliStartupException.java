package io.github.hiwepy.opencode.exception;

import io.github.hiwepy.opencode.cli.availability.OpenCodeCliAvailabilityReport;
import lombok.Getter;

/**
 * 应用启动阶段 OpenCode CLI 不可用且配置为 fail-fast 时抛出。
 */
@Getter
public class OpenCodeCliStartupException extends RuntimeException {

    private final OpenCodeCliAvailabilityReport availabilityReport;

    /**
     * @param message 诊断说明
     * @param report  探测报告
     */
    public OpenCodeCliStartupException(String message, OpenCodeCliAvailabilityReport report) {
        super(message);
        this.availabilityReport = report;
    }
}
