package io.github.easy4j.opencode.cli.availability;

/**
 * Enumeration of possible outcomes from an OpenCode CLI availability probe.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see OpenCodeCliAvailabilityReport
 */
public enum OpenCodeCliAvailabilityStatus {

    /** {@code opencode --version} 探测成功。 */
    AVAILABLE,

    /** 未配置可执行文件。 */
    EXECUTABLE_NOT_CONFIGURED,

    /** 路径不存在或 PATH 中找不到。 */
    EXECUTABLE_NOT_FOUND,

    /** 存在但不可执行。 */
    EXECUTABLE_NOT_EXECUTABLE,

    /** 进程无法启动。 */
    SPAWN_FAILED,

    /** 非零退出。 */
    NON_ZERO_EXIT,

    /** 探测超时。 */
    TIMEOUT,

    /** 其它失败。 */
    FAILED
}
