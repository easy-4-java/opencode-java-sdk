package io.github.easy4j.opencode.cli.availability;

/**
 * Enumeration of possible outcomes from an OpenCode CLI availability probe.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeCliAvailabilityReport
 */
public enum OpenCodeCliAvailabilityStatus {

    /**
     * available枚举值。
     */
    AVAILABLE,

    /**
     * executable not configured枚举值。
     */
    EXECUTABLE_NOT_CONFIGURED,

    /**
     * executable not found枚举值。
     */
    EXECUTABLE_NOT_FOUND,

    /**
     * executable not executable枚举值。
     */
    EXECUTABLE_NOT_EXECUTABLE,

    /**
     * spawn failed枚举值。
     */
    SPAWN_FAILED,

    /**
     * non zero exit枚举值。
     */
    NON_ZERO_EXIT,

    /**
     * timeout枚举值。
     */
    TIMEOUT,

    /**
     * failed枚举值。
     */
    FAILED
}
