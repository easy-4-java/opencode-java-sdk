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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;

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

    /** Optional per-executor concurrency gate; null means unlimited. */
    private final Semaphore concurrencyLimiter;

    /**
     * 创建 open code cli executor 实例，并按传入依赖确定资源所有权。
     *
     * @param config 客户端配置；不得为 {@code null}
     */
    public OpenCodeCliExecutor(OpenCodeCliConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.concurrencyLimiter = config.getMaxConcurrentExecutions() > 0
                ? new Semaphore(config.getMaxConcurrentExecutions(), true) : null;
    }

    /**
     * 同步执行 CLI 命令，返回执行结果。
     *
     * @param args 传递给 OpenCode CLI 的参数数组；每项独立转义
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult execute(String... args) {
        return execute(null, args);
    }

    /**
     * Execute a CLI command with overrides that apply only to this child process.
     *
     * @param context per-execution environment / working-directory overrides; nullable
     * @param args arguments passed to the OpenCode CLI executable
     * @return captured CLI result
     */
    public OpenCodeCliResult execute(OpenCodeCliExecutionContext context, String... args) {
        boolean acquired = false;
        try {
            if (concurrencyLimiter != null) {
                concurrencyLimiter.acquire();
                acquired = true;
            }
            return executeInternal(context, args);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return new OpenCodeCliResult(-1, "", "CLI execution interrupted");
        } finally {
            if (acquired) {
                concurrencyLimiter.release();
            }
        }
    }

    private OpenCodeCliResult executeInternal(OpenCodeCliExecutionContext context, String... args) {
        CommandLine cmd = CommandLine.parse(config.getExecutable());
        for (String arg : args) {
            // Child processes are launched with argv rather than a shell command string.
            cmd.addArgument(arg, false);
        }

        DefaultExecutor executor = new DefaultExecutor();
        BoundedOutputStream stdout = new BoundedOutputStream(config.getMaxStdoutBytes());
        BoundedOutputStream stderr = new BoundedOutputStream(config.getMaxStderrBytes());
        executor.setStreamHandler(new org.apache.commons.exec.PumpStreamHandler(stdout, stderr));

        File workingDirectory = resolveWorkingDirectory(context);
        if (workingDirectory != null) {
            executor.setWorkingDirectory(workingDirectory);
        }

        long timeoutMs = config.getTimeout() * 1000L;
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMs);
        executor.setWatchdog(watchdog);

        try {
            int exitCode;
            Map<String, String> environment = resolveEnvironment(context);
            if (environment == null) {
                exitCode = executor.execute(cmd);
            } else {
                exitCode = executor.execute(cmd, environment);
            }
            return buildResult(exitCode, stdout, stderr, null);
        } catch (IOException error) {
            // Preserve output already produced before spawn/exec/timeout failure.
            return buildResult(-1, stdout, stderr, error.getMessage());
        }
    }

    private OpenCodeCliResult buildResult(int exitCode, BoundedOutputStream stdout,
                                          BoundedOutputStream stderr, String failureMessage) {
        String out = stdout.decodeUtf8().trim();
        String err = stderr.decodeUtf8().trim();
        if (failureMessage != null && !failureMessage.isEmpty() && err.isEmpty()) {
            err = failureMessage;
        }
        if (config.getDebug().allows(HttpLogLevel.BASIC)) {
            log.debug("OpenCode CLI executed: exitCode={}, stdoutLength={}, stderrLength={}, "
                            + "stdoutTruncated={}, stderrTruncated={}",
                    exitCode, out.length(), err.length(), stdout.isTruncated(), stderr.isTruncated());
        }
        if (config.getDebug().allows(HttpLogLevel.BODY)) {
            log.debug("OpenCode CLI output: stdout={}, stderr={}", truncate(out), truncate(err));
        }
        return new OpenCodeCliResult(exitCode, out, err,
                stdout.isTruncated(), stderr.isTruncated());
    }

    private Map<String, String> resolveEnvironment(OpenCodeCliExecutionContext context) {
        if (context == null) {
            return null;
        }
        Map<String, String> overrides = context.getEnvironment();
        if (context.isInheritParentEnvironment() && (overrides == null || overrides.isEmpty())) {
            return null;
        }
        Map<String, String> environment = new HashMap<>();
        if (context.isInheritParentEnvironment()) {
            environment.putAll(System.getenv());
        }
        if (overrides != null) {
            environment.putAll(overrides);
        }
        return environment;
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
        copy.setMaxStdoutBytes(source.getMaxStdoutBytes());
        copy.setMaxStderrBytes(source.getMaxStderrBytes());
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

    private File resolveWorkingDirectory(OpenCodeCliExecutionContext context) {
        String dir = context != null && context.getWorkingDirectory() != null
                ? context.getWorkingDirectory() : config.getWorkingDirectory();
        if (dir == null || dir.trim().isEmpty()) {
            return null;
        }
        return new File(dir.trim());
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private final int maxBytes;
        private boolean truncated;

        private BoundedOutputStream(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public void write(int value) {
            if (maxBytes <= 0 || delegate.size() < maxBytes) {
                delegate.write(value);
            } else {
                truncated = true;
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            if (length <= 0) {
                return;
            }
            if (maxBytes <= 0) {
                delegate.write(bytes, offset, length);
                return;
            }
            int remaining = Math.max(0, maxBytes - delegate.size());
            int retained = Math.min(remaining, length);
            if (retained > 0) {
                delegate.write(bytes, offset, retained);
            }
            if (retained < length) {
                truncated = true;
            }
        }

        private String decodeUtf8() {
            return delegate.toString(StandardCharsets.UTF_8);
        }

        private boolean isTruncated() {
            return truncated;
        }
    }
}
