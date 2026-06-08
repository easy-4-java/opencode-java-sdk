package io.github.hiwepy.opencode.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 本地 {@code opencode} CLI 命令封装 — 覆盖所有官方 CLI 命令。
 *
 * @see <a href="https://opencode.ai/docs/cli/">opencode CLI docs</a>
 */
public class OpenCodeCli {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeCli.class);

    private final OpenCodeCliExecutor executor;

    public OpenCodeCli(OpenCodeCliExecutor executor) {
        this.executor = executor;
    }

    public OpenCodeCliExecutor executor() {
        return executor;
    }

    // ============================================================
    // 全局
    // ============================================================

    /** {@code opencode --version} */
    public OpenCodeCliResult version() {
        return executor.execute("--version");
    }

    /** {@code opencode --help} */
    public OpenCodeCliResult help() {
        return executor.execute("--help");
    }

    // ============================================================
    // run — 执行 prompt
    // ============================================================

    public OpenCodeCliResult run(String message) {
        return executor.execute("run", message);
    }

    public OpenCodeCliResult run(String message, String model) {
        return executor.execute("run", "--model", model, message);
    }

    public OpenCodeCliResult run(String message, String agent, String model) {
        return executor.execute("run", "--agent", agent, "--model", model, message);
    }

    public OpenCodeCliResult runJson(String message) {
        return executor.execute("run", "--format", "json", message);
    }

    public OpenCodeCliResult run(String message, String... extraArgs) {
        String[] args = new String[1 + extraArgs.length];
        args[0] = "run";
        System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
        return executor.execute(args);
    }

    // ============================================================
    // serve / web
    // ============================================================

    /** {@code opencode serve} */
    public OpenCodeCliResult serve() {
        return executor.execute("serve");
    }

    /** {@code opencode serve --port <port>} */
    public OpenCodeCliResult serve(int port) {
        return executor.execute("serve", "--port", String.valueOf(port));
    }

    /** {@code opencode web} */
    public OpenCodeCliResult web() {
        return executor.execute("web");
    }

    // ============================================================
    // session
    // ============================================================

    public OpenCodeCliResult sessionList() {
        return executor.execute("session", "list", "--format", "json");
    }

    public OpenCodeCliResult sessionDelete(String sessionId) {
        return executor.execute("session", "delete", sessionId);
    }

    // ============================================================
    // agent
    // ============================================================

    public OpenCodeCliResult agentList() {
        return executor.execute("agent", "list");
    }

    /** {@code opencode agent create} */
    public OpenCodeCliResult agentCreate() {
        return executor.execute("agent", "create");
    }

    /** {@code opencode agent create <name>} */
    public OpenCodeCliResult agentCreate(String name) {
        return executor.execute("agent", "create", name);
    }

    // ============================================================
    // models
    // ============================================================

    /** {@code opencode models} */
    public OpenCodeCliResult models() {
        return executor.execute("models");
    }

    /** {@code opencode models <provider>} */
    public OpenCodeCliResult models(String provider) {
        return executor.execute("models", provider);
    }

    // ============================================================
    // providers / auth
    // ============================================================

    /** {@code opencode providers list} */
    public OpenCodeCliResult providersList() {
        return executor.execute("providers", "list");
    }

    /** {@code opencode providers login} */
    public OpenCodeCliResult providersLogin() {
        return executor.execute("providers", "login");
    }

    /** {@code opencode providers login <name>} */
    public OpenCodeCliResult providersLogin(String name) {
        return executor.execute("providers", "login", name);
    }

    /** {@code opencode providers logout} */
    public OpenCodeCliResult providersLogout() {
        return executor.execute("providers", "logout");
    }

    /** {@code opencode auth list} */
    public OpenCodeCliResult authList() {
        return executor.execute("auth", "list");
    }

    // ============================================================
    // mcp
    // ============================================================

    public OpenCodeCliResult mcpList() {
        return executor.execute("mcp", "list");
    }

    /** {@code opencode mcp add} */
    public OpenCodeCliResult mcpAdd(String... args) {
        return executor.execute(concat("mcp", "add", args));
    }

    /** {@code opencode mcp auth <name>} */
    public OpenCodeCliResult mcpAuth(String name) {
        return executor.execute("mcp", "auth", name);
    }

    /** {@code opencode mcp logout <name>} */
    public OpenCodeCliResult mcpLogout(String name) {
        return executor.execute("mcp", "logout", name);
    }

    /** {@code opencode mcp debug} */
    public OpenCodeCliResult mcpDebug() {
        return executor.execute("mcp", "debug");
    }

    // ============================================================
    // acp
    // ============================================================

    public OpenCodeCliResult acp() {
        return executor.execute("acp");
    }

    // ============================================================
    // thread / attach
    // ============================================================

    /** {@code opencode thread} */
    public OpenCodeCliResult thread() {
        return executor.execute("thread");
    }

    /** {@code opencode attach} */
    public OpenCodeCliResult attach() {
        return executor.execute("attach");
    }

    // ============================================================
    // generate
    // ============================================================

    public OpenCodeCliResult generate() {
        return executor.execute("generate");
    }

    // ============================================================
    // debug
    // ============================================================

    public OpenCodeCliResult debug() {
        return executor.execute("debug");
    }

    /** {@code opencode debug <subcommand>} */
    public OpenCodeCliResult debug(String subcommand, String... args) {
        return executor.execute(concat("debug", subcommand, args));
    }

    // ============================================================
    // console
    // ============================================================

    /** {@code opencode console login} */
    public OpenCodeCliResult consoleLogin() {
        return executor.execute("console", "login");
    }

    /** {@code opencode console logout} */
    public OpenCodeCliResult consoleLogout() {
        return executor.execute("console", "logout");
    }

    /** {@code opencode console switch} */
    public OpenCodeCliResult consoleSwitch() {
        return executor.execute("console", "switch");
    }

    /** {@code opencode console orgs} */
    public OpenCodeCliResult consoleOrgs() {
        return executor.execute("console", "orgs");
    }

    /** {@code opencode console open} */
    public OpenCodeCliResult consoleOpen() {
        return executor.execute("console", "open");
    }

    // ============================================================
    // stats
    // ============================================================

    public OpenCodeCliResult stats() {
        return executor.execute("stats");
    }

    // ============================================================
    // export / import
    // ============================================================

    public OpenCodeCliResult exportSession() {
        return executor.execute("export");
    }

    public OpenCodeCliResult exportSession(String sessionId) {
        return executor.execute("export", sessionId);
    }

    /** {@code opencode import <file>} */
    public OpenCodeCliResult importSession(String file) {
        return executor.execute("import", file);
    }

    // ============================================================
    // github / pr
    // ============================================================

    /** {@code opencode github install} */
    public OpenCodeCliResult githubInstall() {
        return executor.execute("github", "install");
    }

    /** {@code opencode github run} */
    public OpenCodeCliResult githubRun() {
        return executor.execute("github", "run");
    }

    /** {@code opencode pr <number>} */
    public OpenCodeCliResult pr(String number) {
        return executor.execute("pr", number);
    }

    // ============================================================
    // upgrade / uninstall
    // ============================================================

    public OpenCodeCliResult upgrade() {
        return executor.execute("upgrade");
    }

    public OpenCodeCliResult uninstall() {
        return executor.execute("uninstall");
    }

    // ============================================================
    // plugin
    // ============================================================

    /** {@code opencode plugin <module>} */
    public OpenCodeCliResult plugin(String module) {
        return executor.execute("plugin", module);
    }

    /** {@code opencode plug <module>} (alias) */
    public OpenCodeCliResult plug(String module) {
        return plugin(module);
    }

    // ============================================================
    // db
    // ============================================================

    /** {@code opencode db query <sql>} */
    public OpenCodeCliResult dbQuery(String sql) {
        return executor.execute("db", "query", sql);
    }

    /** {@code opencode db path} */
    public OpenCodeCliResult dbPath() {
        return executor.execute("db", "path");
    }

    // ============================================================
    // completion
    // ============================================================

    /** {@code opencode completion <shell>} */
    public OpenCodeCliResult completion(String shell) {
        return executor.execute("completion", shell);
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private static String[] concat(String first, String second, String... rest) {
        String[] result = new String[2 + rest.length];
        result[0] = first;
        result[1] = second;
        System.arraycopy(rest, 0, result, 2, rest.length);
        return result;
    }
}
