package io.github.easy4j.opencode.cli;

import lombok.Data;

/**
 * Represents the result of a local {@code opencode} CLI command execution.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeCliExecutor
 */
@Data
public class OpenCodeCliResult {

    /**
     * CLI 进程退出状态码；启动失败时为 -1。
     */
    private final int exitCode;
    /**
     * CLI 进程标准输出，已去除首尾空白。
     */
    private final String stdout;
    /**
     * CLI 进程标准错误，已去除首尾空白。
     */
    private final String stderr;

    /**
     * 判断 CLI 进程是否以零状态码正常结束。
     *
     * @return 满足条件返回 {@code true}，否则返回 {@code false}
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }
}
