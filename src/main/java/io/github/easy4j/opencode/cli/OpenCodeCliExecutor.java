package io.github.easy4j.opencode.cli;

import io.github.easy4j.opencode.OpenCodeCliConfig;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Executor for the local {@code opencode} CLI subprocess.
 *
 * <p>Uses Apache Commons Exec to launch the {@code opencode} binary with the configured
 * arguments, capturing stdout and stderr. Supports timeout via {@link ExecuteWatchdog}
 * and working directory configuration.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see OpenCodeCliConfig
 * @see OpenCodeCliResult
 */
public class OpenCodeCliExecutor {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeCliExecutor.class);

    private final OpenCodeCliConfig config;

    /**
     * @param config CLI 配置，不得为 null
     */
    public OpenCodeCliExecutor(OpenCodeCliConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * 同步执行 CLI 命令，返回执行结果。
     */
    public OpenCodeCliResult execute(String... args) {
        CommandLine cmd = CommandLine.parse(config.getExecutable());
        for (String arg : args) {
            cmd.addArgument(arg);
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
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMs);
        executor.setWatchdog(watchdog);

        try {
            int exitCode = executor.execute(cmd);
            String out = stdout.toString().trim();
            String err = stderr.toString().trim();
            log.debug("opencode CLI executed: exitCode={}, stdout={}, stderr={}", exitCode, out, err);
            return new OpenCodeCliResult(exitCode, out, err);
        } catch (IOException e) {
            return new OpenCodeCliResult(-1, "", e.getMessage());
        }
    }

    /**
     * 探测 CLI 是否可用（执行 {@code opencode --version}）。
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
        OpenCodeCliConfig copy = new OpenCodeCliConfig();
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

    private File resolveWorkingDirectory() {
        String dir = config.getWorkingDirectory();
        if (dir == null || dir.trim().isEmpty()) {
            return null;
        }
        return new File(dir.trim());
    }
}
