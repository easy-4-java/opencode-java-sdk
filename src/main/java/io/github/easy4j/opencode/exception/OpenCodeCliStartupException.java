package io.github.easy4j.opencode.exception;

import io.github.easy4j.opencode.cli.availability.OpenCodeCliAvailabilityReport;
import lombok.Getter;

/**
 * Thrown during application startup when the OpenCode CLI is unavailable and fail-fast is enabled.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.cli.availability.OpenCodeCliAvailabilityReport
 */
@Getter
public class OpenCodeCliStartupException extends RuntimeException {

    /**
     * OpenCode 协议字段 {@code availabilityReport}；Java 类型为 {@code OpenCodeCliAvailabilityReport}。
     */
    private final OpenCodeCliAvailabilityReport availabilityReport;

    /**
     * 创建 open code cli startup exception 实例，并按传入依赖确定资源所有权。
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     * @param report 探测报告
     */
    public OpenCodeCliStartupException(String message, OpenCodeCliAvailabilityReport report) {
        super(message);
        this.availabilityReport = report;
    }
}
