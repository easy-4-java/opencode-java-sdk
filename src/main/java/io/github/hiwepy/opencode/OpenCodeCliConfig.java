package io.github.hiwepy.opencode;

import lombok.Data;

/**
 * OpenCode 本地 CLI 客户端配置。
 * <p>
 * 涵盖本地 {@code opencode} 可执行文件路径、超时、工作目录等 CLI 运行时设置。
 * </p>
 */
@Data
public class OpenCodeCliConfig {

    /**
     * 本地可执行文件名或绝对路径。
     */
    private String executable = "opencode";

    /**
     * 本地 CLI 命令超时（秒）。
     */
    private int timeout = 300;

    /**
     * 探测本地运行时是否可用的超时（秒）。
     */
    private int probeTimeoutSeconds = 5;

    /**
     * 本地 CLI 子进程工作目录；为空时使用 JVM 当前目录。
     */
    private String workingDirectory;

    /**
     * 本机 CLI 子进程最大并发数；小于等于 0 时不额外限制。
     */
    private int maxConcurrentExecutions = 0;
}
