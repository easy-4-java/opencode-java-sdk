package io.github.easy4j.opencode.exception;

import io.github.easy4j.opencode.cli.availability.OpenCodeCliAvailabilityReport;
import lombok.Getter;

/**
 * Thrown during application startup when the OpenCode CLI is unavailable and fail-fast is enabled.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see io.github.easy4j.opencode.cli.availability.OpenCodeCliAvailabilityReport
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
