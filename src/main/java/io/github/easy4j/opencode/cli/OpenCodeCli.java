package io.github.easy4j.opencode.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Facade for the local {@code opencode} CLI commands.
 *
 * <p>Wraps an {@link OpenCodeCliExecutor} and provides typed methods for each CLI sub-command
 * (session, agent, models, providers, mcp, etc.).</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see OpenCodeCliExecutor
 * @see OpenCodeCliResult
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

    // ============================================================
    // Version / basic info
    // ============================================================

    /**
     * {@code opencode --version}
     */
    public OpenCodeCliResult version() {
        return executor.execute("--version");
    }

    /**
     * {@code opencode --help}
     */
    public OpenCodeCliResult help() {
        return executor.execute("--help");
    }

    // ============================================================
    // run (non-interactive)
    // ============================================================

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
     * {@code opencode run --format json --session <sessionId> --message <message>}
     */
    public OpenCodeCliResult runJson(String message, String sessionId, String model) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("run", "--format", "json", "--session", sessionId));
        if (model != null) {
            args.add("--model");
            args.add(model);
        }
        args.add(message);
        return executor.execute(args.toArray(new String[0]));
    }

    // ============================================================
    // session
    // ============================================================

    /**
     * {@code opencode session list --format json}
     */
    public OpenCodeCliResult sessionList() {
        return executor.execute("session", "list", "--format", "json");
    }

    /**
     * {@code opencode session list --max-count <n> --format json}
     */
    public OpenCodeCliResult sessionList(int maxCount) {
        return executor.execute("session", "list", "--max-count", String.valueOf(maxCount),
                "--format", "json");
    }

    /**
     * {@code opencode session delete <sessionId>}
     */
    public OpenCodeCliResult sessionDelete(String sessionId) {
        return executor.execute("session", "delete", sessionId);
    }

    // ============================================================
    // agent
    // ============================================================

    /**
     * {@code opencode agent list}
     */
    public OpenCodeCliResult agentList() {
        return executor.execute("agent", "list");
    }

    /**
     * {@code opencode agent create --path ... --description ... --mode ... --tools ...}
     */
    public OpenCodeCliResult agentCreate(String path, String description, String mode,
                                         String permissions, String model) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("agent", "create"));
        if (path != null) {
            args.add("--path");
            args.add(path);
        }
        if (description != null) {
            args.add("--description");
            args.add(description);
        }
        if (mode != null) {
            args.add("--mode");
            args.add(mode);
        }
        if (permissions != null) {
            args.add("--tools");
            args.add(permissions);
        }
        if (model != null) {
            args.add("--model");
            args.add(model);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    // ============================================================
    // models / providers / auth
    // ============================================================

    /**
     * {@code opencode models}
     */
    public OpenCodeCliResult models() {
        return executor.execute("models");
    }

    /**
     * {@code opencode models <provider> [--verbose] [--refresh]}
     */
    public OpenCodeCliResult models(String provider, boolean verbose, boolean refresh) {
        List<String> args = new ArrayList<>();
        args.add("models");
        if (provider != null) {
            args.add(provider);
        }
        if (verbose) {
            args.add("--verbose");
        }
        if (refresh) {
            args.add("--refresh");
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode providers list}
     */
    public OpenCodeCliResult providersList() {
        return executor.execute("providers", "list");
    }

    /**
     * {@code opencode providers login [--provider ... --method ...]}
     */
    public OpenCodeCliResult providersLogin(String provider, String method) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("providers", "login"));
        if (provider != null) {
            args.add("--provider");
            args.add(provider);
        }
        if (method != null) {
            args.add("--method");
            args.add(method);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode providers logout [provider]}
     */
    public OpenCodeCliResult providersLogout(String provider) {
        if (provider == null) {
            return executor.execute("providers", "logout");
        }
        return executor.execute("providers", "logout", provider);
    }

    /**
     * {@code opencode auth list}（与 providers list 等价）
     */
    public OpenCodeCliResult authList() {
        return executor.execute("auth", "list");
    }

    /**
     * {@code opencode auth login [--provider ... --method ...]}
     */
    public OpenCodeCliResult authLogin(String provider, String method) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("auth", "login"));
        if (provider != null) {
            args.add("--provider");
            args.add(provider);
        }
        if (method != null) {
            args.add("--method");
            args.add(method);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode auth logout [provider]}
     */
    public OpenCodeCliResult authLogout(String provider) {
        if (provider == null) {
            return executor.execute("auth", "logout");
        }
        return executor.execute("auth", "logout", provider);
    }

    // ============================================================
    // mcp
    // ============================================================

    /**
     * {@code opencode mcp list}
     */
    public OpenCodeCliResult mcpList() {
        return executor.execute("mcp", "list");
    }

    /**
     * {@code opencode mcp add <name> ...} 远程 MCP server（url 形式）。
     */
    public OpenCodeCliResult mcpAdd(String name, String url) {
        return executor.execute("mcp", "add", name, "--url", url);
    }

    /**
     * {@code opencode mcp add <name>} 后接本地命令（exec 形式）。
     * <p>命令经 {@code --} 透传给 opencode，例如 {@code opencode mcp add foo -- npx server}</p>
     */
    public OpenCodeCliResult mcpAddLocal(String name, String... localCmd) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("mcp", "add", name));
        args.add("--");
        Collections.addAll(args, localCmd);
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode mcp logout [name]}
     */
    public OpenCodeCliResult mcpLogout(String name) {
        if (name == null) {
            return executor.execute("mcp", "logout");
        }
        return executor.execute("mcp", "logout", name);
    }

    /**
     * {@code opencode mcp auth [name]}
     */
    public OpenCodeCliResult mcpAuth(String name) {
        if (name == null) {
            return executor.execute("mcp", "auth");
        }
        return executor.execute("mcp", "auth", name);
    }

    /**
     * {@code opencode mcp debug <name>}
     */
    public OpenCodeCliResult mcpDebug(String name) {
        return executor.execute("mcp", "debug", name);
    }

    // ============================================================
    // upgrade / uninstall / install
    // ============================================================

    /**
     * {@code opencode upgrade}
     */
    public OpenCodeCliResult upgrade() {
        return executor.execute("upgrade");
    }

    /**
     * {@code opencode upgrade <target> [--method ...]}
     */
    public OpenCodeCliResult upgrade(String target, String method) {
        List<String> args = new ArrayList<>();
        args.add("upgrade");
        if (target != null) {
            args.add(target);
        }
        if (method != null) {
            args.add("--method");
            args.add(method);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode uninstall [--keep-config] [--keep-data] [--dry-run] [--force]}
     */
    public OpenCodeCliResult uninstall(boolean keepConfig, boolean keepData,
                                       boolean dryRun, boolean force) {
        List<String> args = new ArrayList<>();
        args.add("uninstall");
        if (keepConfig) {
            args.add("--keep-config");
        }
        if (keepData) {
            args.add("--keep-data");
        }
        if (dryRun) {
            args.add("--dry-run");
        }
        if (force) {
            args.add("--force");
        }
        return executor.execute(args.toArray(new String[0]));
    }

    // ============================================================
    // stats / export / import / db / debug
    // ============================================================

    /**
     * {@code opencode stats [--days ... --tools ... --models ... --project ...]}
     */
    public OpenCodeCliResult stats(Integer days, Integer tools, Integer models, String project) {
        List<String> args = new ArrayList<>();
        args.add("stats");
        if (days != null) {
            args.add("--days");
            args.add(String.valueOf(days));
        }
        if (tools != null) {
            args.add("--tools");
            args.add(String.valueOf(tools));
        }
        if (models != null) {
            args.add("--models");
            args.add(String.valueOf(models));
        }
        if (project != null) {
            args.add("--project");
            args.add(project);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode export [sessionID] [--sanitize]}
     */
    public OpenCodeCliResult export(String sessionId, boolean sanitize) {
        List<String> args = new ArrayList<>();
        args.add("export");
        if (sessionId != null) {
            args.add(sessionId);
        }
        if (sanitize) {
            args.add("--sanitize");
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode import <file>}
     */
    public OpenCodeCliResult importSession(String fileOrUrl) {
        return executor.execute("import", fileOrUrl);
    }

    /**
     * {@code opencode db [query] [--format json|tsv]}
     */
    public OpenCodeCliResult db(String query, String format) {
        List<String> args = new ArrayList<>();
        args.add("db");
        if (query != null) {
            args.add(query);
        }
        if (format != null) {
            args.add("--format");
            args.add(format);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode db path}
     */
    public OpenCodeCliResult dbPath() {
        return executor.execute("db", "path");
    }

    /**
     * {@code opencode debug config}
     */
    public OpenCodeCliResult debugConfig() {
        return executor.execute("debug", "config");
    }

    /**
     * {@code opencode debug paths}
     */
    public OpenCodeCliResult debugPaths() {
        return executor.execute("debug", "paths");
    }

    /**
     * {@code opencode debug info}
     */
    public OpenCodeCliResult debugInfo() {
        return executor.execute("debug", "info");
    }

    /**
     * {@code opencode debug scrap}
     */
    public OpenCodeCliResult debugScrap() {
        return executor.execute("debug", "scrap");
    }

    /**
     * {@code opencode debug skill}
     */
    public OpenCodeCliResult debugSkill() {
        return executor.execute("debug", "skill");
    }

    /**
     * {@code opencode debug startup}
     */
    public OpenCodeCliResult debugStartup() {
        return executor.execute("debug", "startup");
    }

    // ============================================================
    // serve / web / acp / attach / generate
    // ============================================================

    /**
     * {@code opencode serve [--port N --hostname H]}
     */
    public OpenCodeCliResult serve(Integer port, String hostname) {
        List<String> args = new ArrayList<>();
        args.add("serve");
        if (port != null) {
            args.add("--port");
            args.add(String.valueOf(port));
        }
        if (hostname != null) {
            args.add("--hostname");
            args.add(hostname);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode web [--port N --hostname H]}
     */
    public OpenCodeCliResult web(Integer port, String hostname) {
        List<String> args = new ArrayList<>();
        args.add("web");
        if (port != null) {
            args.add("--port");
            args.add(String.valueOf(port));
        }
        if (hostname != null) {
            args.add("--hostname");
            args.add(hostname);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode acp [--cwd ...]}
     */
    public OpenCodeCliResult acp(String cwd) {
        List<String> args = new ArrayList<>();
        args.add("acp");
        if (cwd != null) {
            args.add("--cwd");
            args.add(cwd);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode attach <url> [--dir ... --session ... --username ... --password ...]}
     */
    public OpenCodeCliResult attach(String url, String dir, String sessionId,
                                    String username, String password) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("attach", url));
        if (dir != null) {
            args.add("--dir");
            args.add(dir);
        }
        if (sessionId != null) {
            args.add("--session");
            args.add(sessionId);
        }
        if (username != null) {
            args.add("--username");
            args.add(username);
        }
        if (password != null) {
            args.add("--password");
            args.add(password);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode generate}
     * <p>打印带 JS 代码示例注入的 OpenAPI spec。</p>
     */
    public OpenCodeCliResult generate() {
        return executor.execute("generate");
    }

    // ============================================================
    // github / pr
    // ============================================================

    /**
     * {@code opencode github install}
     */
    public OpenCodeCliResult githubInstall() {
        return executor.execute("github", "install");
    }

    /**
     * {@code opencode github run [--event ... --token ...]}
     */
    public OpenCodeCliResult githubRun(String event, String token) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("github", "run"));
        if (event != null) {
            args.add("--event");
            args.add(event);
        }
        if (token != null) {
            args.add("--token");
            args.add(token);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    /**
     * {@code opencode pr <number>}
     */
    public OpenCodeCliResult pr(int number) {
        return executor.execute("pr", String.valueOf(number));
    }

    // ============================================================
    // plugin (alias: plug)
    // ============================================================

    /**
     * {@code opencode plugin <module> [--global] [--force]}
     */
    public OpenCodeCliResult plugin(String module, boolean global, boolean force) {
        List<String> args = new ArrayList<>();
        args.add("plugin");
        args.add(module);
        if (global) {
            args.add("--global");
        }
        if (force) {
            args.add("--force");
        }
        return executor.execute(args.toArray(new String[0]));
    }

    // ============================================================
    // console (account)
    // ============================================================

    /**
     * {@code opencode console login [url]}
     */
    public OpenCodeCliResult consoleLogin(String url) {
        if (url == null) {
            return executor.execute("console", "login");
        }
        return executor.execute("console", "login", url);
    }

    /**
     * {@code opencode console logout [email]}
     */
    public OpenCodeCliResult consoleLogout(String email) {
        if (email == null) {
            return executor.execute("console", "logout");
        }
        return executor.execute("console", "logout", email);
    }

    /**
     * {@code opencode console orgs}
     */
    public OpenCodeCliResult consoleOrgs() {
        return executor.execute("console", "orgs");
    }

    /**
     * {@code opencode console switch}
     */
    public OpenCodeCliResult consoleSwitch() {
        return executor.execute("console", "switch");
    }

    /**
     * {@code opencode console open}
     */
    public OpenCodeCliResult consoleOpen() {
        return executor.execute("console", "open");
    }
}