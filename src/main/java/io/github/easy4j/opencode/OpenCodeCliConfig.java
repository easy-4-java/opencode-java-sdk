package io.github.easy4j.opencode;

import lombok.Data;

/**
 * Configuration for the local OpenCode CLI subsystem.
 * <p>Covers local {@code opencode} executable path, timeouts, working directory,
 * and other CLI runtime settings. Can be mapped from Spring
 * {@code @ConfigurationProperties}.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeClient
 * @see OpenCodeClientConfig
 */
@Data
public class OpenCodeCliConfig {

    /**
     * 是否启用本地 CLI 子系统。
     * <p>为 false 时跳过 CLI 相关初始化和检查。</p>
     */
    private boolean enabled = true;

    /**
     * 启动时是否探测 {@code opencode --version}。
     */
    private boolean startupCheckEnabled = false;

    /**
     * CLI 不可用时是否快速失败（中断构造）。
     * <p>默认 false 仅打 WARN；生产环境建议设为 true。</p>
     */
    private boolean failFastOnUnavailable = false;

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
