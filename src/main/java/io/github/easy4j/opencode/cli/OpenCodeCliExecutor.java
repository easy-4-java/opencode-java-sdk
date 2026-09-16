package io.github.easy4j.opencode.cli;

import io.github.easy4j.opencode.OpenCodeCliConfig;
import okhttp3.extension.logging.HttpLogLevel;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Executor for the local {@code opencode} CLI subprocess.
 * <p>Uses Apache Commons Exec to launch the {@code opencode} binary with the configured
 * arguments, capturing stdout and stderr. Supports timeout via {@link ExecuteWatchdog}
 * and working directory configuration.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeCliConfig
 * @see OpenCodeCliResult
 */
public class OpenCodeCliExecutor {

    /**
     * 当前组件使用的 SLF4J 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(OpenCodeCliExecutor.class);

    /**
     * 当前客户端使用的不可变配置引用。
     */
    private final OpenCodeCliConfig config;

    /**
     * 创建 open code cli executor 实例，并按传入依赖确定资源所有权。
     *
     * @param config 客户端配置；不得为 {@code null}
     */
    public OpenCodeCliExecutor(OpenCodeCliConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * 同步执行 CLI 命令，返回执行结果。
     *
     * @param args 传递给 OpenCode CLI 的参数数组；每项独立转义
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult execute(String... args) {
        CommandLine cmd = CommandLine.parse(config.getExecutable());
        for (String arg : args) {
            // handleQuoting=false：子进程经 exec(argv) 启动而非 shell，
            // commons-exec 默认会把含空格参数包上字面双引号烤进 argv，
            // 导致多词 prompt 以带引号形态到达 opencode。
            cmd.addArgument(arg, false);
        }

        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        executor.setStreamHandler(new org.apache.commons.exec.PumpStreamHandler(stdout, stderr));

        File workingDirectory = resolveWorkingDirectory();
        if (workingDirectory != null) {
            executor.setWorkingDirectory(workingDirectory);
        }

        long timeoutMs = config.getTimeout() * 1000L;
        // Watchdog 在超时后终止子进程；同步 CLI 边界与 OkHttp Dispatcher 相互独立。
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMs);
        executor.setWatchdog(watchdog);

        try {
            int exitCode = executor.execute(cmd);
            // 显式 UTF-8 解码：toString() 走平台默认字符集，GBK 默认字符集的
            // Windows 上会把 opencode 的 UTF-8 输出解成乱码。
            String out = stdout.toString(StandardCharsets.UTF_8).trim();
            String err = stderr.toString(StandardCharsets.UTF_8).trim();
            if (config.getDebug().allows(HttpLogLevel.BASIC)) {
                log.debug("OpenCode CLI executed: exitCode={}, stdoutLength={}, stderrLength={}",
                        exitCode, out.length(), err.length());
            }
            if (config.getDebug().allows(HttpLogLevel.BODY)) {
                log.debug("OpenCode CLI output: stdout={}, stderr={}", truncate(out), truncate(err));
            }
            return new OpenCodeCliResult(exitCode, out, err);
        } catch (IOException e) {
            return new OpenCodeCliResult(-1, "", e.getMessage());
        }
    }

    /**
     * 探测 CLI 是否可用（执行 {@code opencode --version}）。
     *
     * @return 操作成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean probe() {
        try {
            OpenCodeCliConfig probeConfig = copyForProbe(config);
            OpenCodeCliResult result = new OpenCodeCliExecutor(probeConfig).execute("--version");
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    private static OpenCodeCliConfig copyForProbe(OpenCodeCliConfig source) {
        OpenCodeCliConfig copy = new OpenCodeCliConfig(source.getDebug());
        copy.setExecutable(source.getExecutable());
        copy.setWorkingDirectory(source.getWorkingDirectory());
        copy.setMaxConcurrentExecutions(source.getMaxConcurrentExecutions());
        int probeSec = source.getProbeTimeoutSeconds();
        if (probeSec <= 0) {
            probeSec = 5;
        }
        copy.setTimeout(probeSec);
        copy.setProbeTimeoutSeconds(probeSec);
        return copy;
    }

    private String truncate(String content) {
        int maxLength = config.getDebug().resolveMaxContentLength();
        return content.length() <= maxLength ? content : content.substring(0, maxLength) + "...<truncated>";
    }

    private File resolveWorkingDirectory() {
        String dir = config.getWorkingDirectory();
        if (dir == null || dir.trim().isEmpty()) {
            return null;
        }
        return new File(dir.trim());
    }
}
