package io.github.hiwepy.opencode.cli;

import io.github.hiwepy.opencode.OpenCodeClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地 {@code opencode} CLI 命令封装。
 *
 * @see <a href="https://opencode.ai/docs/cli/">opencode CLI docs</a>
 */
public class OpenCodeCli {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeCli.class);

    private final OpenCodeCliExecutor executor;

    public OpenCodeCli(OpenCodeCliExecutor executor) {
        this.executor = executor;
    }

    /**
     * 获取 CLI 执行器（用于自定义命令）。
     */
    public OpenCodeCliExecutor executor() {
        return executor;
    }

    /**
     * {@code opencode --version}
     */
    public OpenCodeCliResult version() {
        return executor.execute("--version");
    }

    /**
     * {@code opencode run <message>}
     * <p>非交互模式执行 prompt，返回 AI 响应。</p>
     */
    public OpenCodeCliResult run(String message) {
        return executor.execute("run", message);
    }

    /**
     * {@code opencode run --model <model> <message>}
     */
    public OpenCodeCliResult run(String message, String model) {
        return executor.execute("run", "--model", model, message);
    }

    /**
     * {@code opencode run --agent <agent> --model <model> <message>}
     */
    public OpenCodeCliResult run(String message, String agent, String model) {
        return executor.execute("run", "--agent", agent, "--model", model, message);
    }

    /**
     * {@code opencode run --format json <message>}
     * <p>返回 JSON 格式的原始事件流。</p>
     */
    public OpenCodeCliResult runJson(String message) {
        return executor.execute("run", "--format", "json", message);
    }

    /**
     * {@code opencode session list --format json}
     */
    public OpenCodeCliResult sessionList() {
        return executor.execute("session", "list", "--format", "json");
    }

    /**
     * {@code opencode session delete <sessionId>}
     */
    public OpenCodeCliResult sessionDelete(String sessionId) {
        return executor.execute("session", "delete", sessionId);
    }

    /**
     * {@code opencode agent list}
     */
    public OpenCodeCliResult agentList() {
        return executor.execute("agent", "list");
    }

    /**
     * {@code opencode models}
     */
    public OpenCodeCliResult models() {
        return executor.execute("models");
    }

    /**
     * {@code opencode mcp list}
     */
    public OpenCodeCliResult mcpList() {
        return executor.execute("mcp", "list");
    }

    /**
     * {@code opencode auth list}
     */
    public OpenCodeCliResult authList() {
        return executor.execute("auth", "list");
    }
}
