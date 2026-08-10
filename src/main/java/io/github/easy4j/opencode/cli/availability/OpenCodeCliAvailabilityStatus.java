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
     * CLI 可执行文件存在且版本探测命令成功。
     */
    AVAILABLE,

    /**
     * 配置中没有提供 CLI 可执行文件名称或路径。
     */
    EXECUTABLE_NOT_CONFIGURED,

    /**
     * 在配置路径和系统 PATH 中均未找到可执行文件。
     */
    EXECUTABLE_NOT_FOUND,

    /**
     * 目标文件存在，但当前进程没有执行权限。
     */
    EXECUTABLE_NOT_EXECUTABLE,

    /**
     * 操作系统拒绝或无法启动 CLI 子进程。
     */
    SPAWN_FAILED,

    /**
     * 版本探测命令已运行，但以非零状态码退出。
     */
    NON_ZERO_EXIT,

    /**
     * 版本探测命令未在配置的超时时间内结束。
     */
    TIMEOUT,

    /**
     * 探测过程中发生未归类的异常。
     */
    FAILED
}
