package io.github.easy4j.opencode.cli.availability;

import io.github.easy4j.opencode.OpenCodeCliConfig;
import io.github.easy4j.opencode.cli.OpenCodeCli;
import io.github.easy4j.opencode.cli.OpenCodeCliExecutor;
import io.github.easy4j.opencode.cli.OpenCodeCliResult;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

/**
 * Checks whether the local {@code opencode} CLI is installed and can execute {@code opencode --version}.
 * <p>Resolves the configured executable path (absolute/relative or via {@code PATH}),
 * then runs a version probe with a short timeout. Returns an {@link OpenCodeCliAvailabilityReport}
 * with the detailed status.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeCliAvailabilityReport
 * @see OpenCodeCliAvailabilityStatus
 */
public class OpenCodeCliAvailabilityChecker {

    /**
     * 使用与运行时一致的配置探测 CLI。
     *
     * @param config 客户端配置；不得为 {@code null}
     * @return OpenCode SDK 返回的CLI 可用性报告对象
     */
    public OpenCodeCliAvailabilityReport check(OpenCodeCliConfig config) {
        Objects.requireNonNull(config, "config");
        String configured = config.getExecutable();
        if (isBlank(configured)) {
            return unavailable(
                    OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED,
                    configured,
                    null,
                    "opencode.cli.executable is blank",
                    null);
        }
        String trimmed = configured.trim();
        Optional<String> resolved = resolveExecutablePath(trimmed);
        if (!resolved.isPresent()) {
            if (looksLikePath(trimmed)) {
                File file = new File(trimmed);
                if (!file.exists()) {
                    return unavailable(
                            OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_FOUND,
                            trimmed,
                            null,
                            "executable file does not exist: " + file.getAbsolutePath(),
                            null);
                }
                return unavailable(
                        OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_EXECUTABLE,
                        trimmed,
                        file.getAbsolutePath(),
                        "executable exists but is not executable: " + file.getAbsolutePath(),
                        null);
            }
            return unavailable(
                    OpenCodeCliAvailabilityStatus.EXECUTABLE_NOT_FOUND,
                    trimmed,
                    null,
                    "executable not found on PATH: " + trimmed,
                    null);
        }

        OpenCodeCliConfig probeConfig = copyForProbe(config);
        OpenCodeCliExecutor probeExecutor = new OpenCodeCliExecutor(probeConfig);
        OpenCodeCliResult result = new OpenCodeCli(probeExecutor).version();
        if (result.isSuccess()) {
            return OpenCodeCliAvailabilityReport.builder()
                    .status(OpenCodeCliAvailabilityStatus.AVAILABLE)
                    .available(true)
                    .configuredExecutable(trimmed)
                    .resolvedExecutablePath(resolved.get())
                    .message("opencode --version succeeded")
                    .probeResult(result)
                    .build();
        }
        if (result.getExitCode() == -1 && containsTimeoutHint(result)) {
            return unavailable(
                    OpenCodeCliAvailabilityStatus.TIMEOUT,
                    trimmed,
                    resolved.get(),
                    "opencode --version timed out",
                    result);
        }
        if (result.getExitCode() == -1 && isSpawnFailure(result)) {
            return unavailable(
                    OpenCodeCliAvailabilityStatus.SPAWN_FAILED,
                    trimmed,
                    resolved.get(),
                    firstLine(result.getStderr()),
                    result);
        }
        return unavailable(
                OpenCodeCliAvailabilityStatus.NON_ZERO_EXIT,
                trimmed,
                resolved.get(),
                "opencode --version exitCode=" + result.getExitCode(),
                result);
    }

    /**
     * 解析可执行文件：绝对/相对路径直接检查；否则在 {@code PATH} 中查找。
     */
    static Optional<String> resolveExecutablePath(String executable) {
        if (isBlank(executable)) {
            return Optional.empty();
        }
        String trimmed = executable.trim();
        File direct = new File(trimmed);
        if (looksLikePath(trimmed)) {
            if (direct.isFile() && direct.canExecute()) {
                return Optional.of(direct.getAbsolutePath());
            }
            return Optional.empty();
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return Optional.empty();
        }
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (isBlank(dir)) {
                continue;
            }
            File candidate = new File(dir.trim(), trimmed);
            if (candidate.isFile() && candidate.canExecute()) {
                return Optional.of(candidate.getAbsolutePath());
            }
        }
        return Optional.empty();
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

    private static boolean looksLikePath(String executable) {
        return executable.contains("/") || executable.contains("\\") || new File(executable).isAbsolute();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean containsTimeoutHint(OpenCodeCliResult result) {
        String combined = result.getStderr() + result.getStdout();
        return combined.toLowerCase().contains("timed out");
    }

    private static boolean isSpawnFailure(OpenCodeCliResult result) {
        String stderr = result.getStderr();
        return stderr != null
                && (stderr.contains("could not be started")
                || stderr.contains("No such file")
                || stderr.contains("spawn"));
    }

    private static String firstLine(String text) {
        if (isBlank(text)) {
            return "opencode execution failed";
        }
        int idx = text.indexOf('\n');
        return idx >= 0 ? text.substring(0, idx) : text;
    }

    private static OpenCodeCliAvailabilityReport unavailable(
            OpenCodeCliAvailabilityStatus status,
            String configured,
            String resolved,
            String message,
            OpenCodeCliResult partial) {
        return OpenCodeCliAvailabilityReport.builder()
                .status(status)
                .available(false)
                .configuredExecutable(configured)
                .resolvedExecutablePath(resolved)
                .message(message)
                .probeResult(partial)
                .build();
    }
}
