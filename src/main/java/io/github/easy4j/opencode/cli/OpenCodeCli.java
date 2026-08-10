package io.github.easy4j.opencode.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Facade for the local {@code opencode} CLI commands.
 * <p>Wraps an {@link OpenCodeCliExecutor} and provides typed methods for each CLI sub-command
 * (session, agent, models, providers, mcp, etc.).</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see OpenCodeCliExecutor
 * @see OpenCodeCliResult
 * @see <a href="https://opencode.ai/docs/cli/">opencode CLI docs</a>
 */
public class OpenCodeCli {

    /**
     * 当前组件使用的 SLF4J 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(OpenCodeCli.class);

    /**
     * OpenCode 协议字段 {@code executor}；Java 类型为 {@code OpenCodeCliExecutor}。
     */
    private final OpenCodeCliExecutor executor;

    /**
     * 创建 open code cli 实例，并按传入依赖确定资源所有权。
     *
     * @param executor 负责启动子进程并收集输出的 CLI 执行器
     */
    public OpenCodeCli(OpenCodeCliExecutor executor) {
        this.executor = executor;
    }

    /**
     * 获取 CLI 执行器（用于自定义命令）。
     *
     * @return OpenCode SDK 返回的CLI 执行器对象
     */
    public OpenCodeCliExecutor executor() {
        return executor;
    }

    // ============================================================
    // Version / basic info
    // ============================================================

    /**
     * {@code opencode --version}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult version() {
        return executor.execute("--version");
    }

    /**
     * {@code opencode --help}
     *
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult run(String message) {
        return executor.execute("run", message);
    }

    /**
     * {@code opencode run --model <model> <message>}
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     * @param model 模型标识，通常采用 provider/model 格式；为空时使用默认模型
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult run(String message, String model) {
        return executor.execute("run", "--model", model, message);
    }

    /**
     * {@code opencode run --agent <agent> --model <model> <message>}
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     * @param agent 执行请求的智能体名称；为空时使用服务端默认智能体
     * @param model 模型标识，通常采用 provider/model 格式；为空时使用默认模型
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult run(String message, String agent, String model) {
        return executor.execute("run", "--agent", agent, "--model", model, message);
    }

    /**
     * {@code opencode run --format json <message>}
     * <p>返回 JSON 格式的原始事件流。</p>
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult runJson(String message) {
        return executor.execute("run", "--format", "json", message);
    }

    /**
     * {@code opencode run --format json --session <sessionId> --message <message>}
     *
     * @param message 传递给 OpenCode CLI 的提示文本
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param model 模型标识，通常采用 provider/model 格式；为空时使用默认模型
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult sessionList() {
        return executor.execute("session", "list", "--format", "json");
    }

    /**
     * {@code opencode session list --max-count <n> --format json}
     *
     * @param maxCount 最大返回会话数量
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult sessionList(int maxCount) {
        return executor.execute("session", "list", "--max-count", String.valueOf(maxCount),
                "--format", "json");
    }

    /**
     * {@code opencode session delete <sessionId>}
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult sessionDelete(String sessionId) {
        return executor.execute("session", "delete", sessionId);
    }

    // ============================================================
    // agent
    // ============================================================

    /**
     * {@code opencode agent list}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult agentList() {
        return executor.execute("agent", "list");
    }

    /**
     * {@code opencode agent create --path ... --description ... --mode ... --tools ...}
     *
     * @param path 文件或工作目录路径
     * @param description 资源的可读说明；为空时由 OpenCode 使用默认描述
     * @param mode 智能体运行模式或 CLI 行为模式
     * @param permissions 智能体创建时使用的权限配置文本
     * @param model 模型标识，通常采用 provider/model 格式；为空时使用默认模型
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult models() {
        return executor.execute("models");
    }

    /**
     * {@code opencode models <provider> [--verbose] [--refresh]}
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @param verbose 是否输出模型的详细元数据
     * @param refresh 是否在列出模型前强制刷新提供方元数据
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult providersList() {
        return executor.execute("providers", "list");
    }

    /**
     * {@code opencode providers login [--provider ... --method ...]}
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @param method 认证或升级方式
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult providersLogout(String provider) {
        if (provider == null) {
            return executor.execute("providers", "logout");
        }
        return executor.execute("providers", "logout", provider);
    }

    /**
     * {@code opencode auth list}（与 providers list 等价）
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult authList() {
        return executor.execute("auth", "list");
    }

    /**
     * {@code opencode auth login [--provider ... --method ...]}
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @param method 认证或升级方式
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param provider 模型提供方 ID；为空时不限制提供方
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult mcpList() {
        return executor.execute("mcp", "list");
    }

    /**
     * {@code opencode mcp add <name> ...} 远程 MCP server（url 形式）。
     *
     * @param name 资源名称
     * @param url 远程服务或控制台 URL
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult mcpAdd(String name, String url) {
        return executor.execute("mcp", "add", name, "--url", url);
    }

    /**
     * {@code opencode mcp add <name>} 后接本地命令（exec 形式）。
     * <p>命令经 {@code --} 透传给 opencode，例如 {@code opencode mcp add foo -- npx server}</p>
     *
     * @param name 资源名称
     * @param localCmd 追加到命令行的可变参数；每个元素作为独立参数传递
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param name 资源名称
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult mcpLogout(String name) {
        if (name == null) {
            return executor.execute("mcp", "logout");
        }
        return executor.execute("mcp", "logout", name);
    }

    /**
     * {@code opencode mcp auth [name]}
     *
     * @param name 资源名称
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult mcpAuth(String name) {
        if (name == null) {
            return executor.execute("mcp", "auth");
        }
        return executor.execute("mcp", "auth", name);
    }

    /**
     * {@code opencode mcp debug <name>}
     *
     * @param name 资源名称
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult mcpDebug(String name) {
        return executor.execute("mcp", "debug", name);
    }

    // ============================================================
    // upgrade / uninstall / install
    // ============================================================

    /**
     * {@code opencode upgrade}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult upgrade() {
        return executor.execute("upgrade");
    }

    /**
     * {@code opencode upgrade <target> [--method ...]}
     *
     * @param target 升级目标版本；为空时由 CLI 选择最新版本
     * @param method 认证或升级方式
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param keepConfig 卸载时是否保留本地配置
     * @param keepData 卸载时是否保留会话和缓存数据
     * @param dryRun 是否仅预览操作而不实际修改本地安装
     * @param force 是否强制执行操作
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param days 统计覆盖的最近天数；为 {@code null} 时使用 CLI 默认范围
     * @param tools 统计结果中最多展示的工具数量；为 {@code null} 时使用 CLI 默认值
     * @param models 统计结果中最多展示的模型数量；为 {@code null} 时使用 CLI 默认值
     * @param project 统计限定的项目名称；为空时统计全部项目
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param sanitize 是否在导出结果中移除敏感信息
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param fileOrUrl 待导入的本地文件路径或远程 URL
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult importSession(String fileOrUrl) {
        return executor.execute("import", fileOrUrl);
    }

    /**
     * {@code opencode db [query] [--format json|tsv]}
     *
     * @param query 搜索或数据库查询表达式
     * @param format 输出格式名称；为空时使用 CLI 默认格式
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult dbPath() {
        return executor.execute("db", "path");
    }

    /**
     * {@code opencode debug config}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult debugConfig() {
        return executor.execute("debug", "config");
    }

    /**
     * {@code opencode debug paths}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult debugPaths() {
        return executor.execute("debug", "paths");
    }

    /**
     * {@code opencode debug info}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult debugInfo() {
        return executor.execute("debug", "info");
    }

    /**
     * {@code opencode debug scrap}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult debugScrap() {
        return executor.execute("debug", "scrap");
    }

    /**
     * {@code opencode debug skill}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult debugSkill() {
        return executor.execute("debug", "skill");
    }

    /**
     * {@code opencode debug startup}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult debugStartup() {
        return executor.execute("debug", "startup");
    }

    // ============================================================
    // serve / web / acp / attach / generate
    // ============================================================

    /**
     * {@code opencode serve [--port N --hostname H]}
     *
     * @param port 监听端口；为 {@code null} 时使用 CLI 默认值
     * @param hostname 监听地址；为空时使用 CLI 默认值
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param port 监听端口；为 {@code null} 时使用 CLI 默认值
     * @param hostname 监听地址；为空时使用 CLI 默认值
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param cwd CLI 进程使用的当前工作目录；为空时继承配置
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param url 远程服务或控制台 URL
     * @param dir CLI 命令作用目录；为空时使用配置工作目录
     * @param sessionId OpenCode 会话 ID；不得为空
     * @param username OpenCode Server Basic Auth 用户名
     * @param password OpenCode Server Basic Auth 密码；日志中不得明文输出
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult generate() {
        return executor.execute("generate");
    }

    // ============================================================
    // github / pr
    // ============================================================

    /**
     * {@code opencode github install}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult githubInstall() {
        return executor.execute("github", "install");
    }

    /**
     * {@code opencode github run [--event ... --token ...]}
     *
     * @param event 触发当前回调的完整 SSE 事件
     * @param token GitHub 访问令牌；为空时由 CLI 自行解析
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param number GitHub Pull Request 编号
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult pr(int number) {
        return executor.execute("pr", String.valueOf(number));
    }

    // ============================================================
    // plugin (alias: plug)
    // ============================================================

    /**
     * {@code opencode plugin <module> [--global] [--force]}
     *
     * @param module 插件模块名称或安装来源
     * @param global 是否在全局作用域安装插件
     * @param force 是否强制执行操作
     * @return CLI 的退出状态、标准输出和错误输出
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
     *
     * @param url 远程服务或控制台 URL
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult consoleLogin(String url) {
        if (url == null) {
            return executor.execute("console", "login");
        }
        return executor.execute("console", "login", url);
    }

    /**
     * {@code opencode console logout [email]}
     *
     * @param email 需要退出登录的控制台账号邮箱；为空时使用当前账号
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult consoleLogout(String email) {
        if (email == null) {
            return executor.execute("console", "logout");
        }
        return executor.execute("console", "logout", email);
    }

    /**
     * {@code opencode console orgs}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult consoleOrgs() {
        return executor.execute("console", "orgs");
    }

    /**
     * {@code opencode console switch}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult consoleSwitch() {
        return executor.execute("console", "switch");
    }

    /**
     * {@code opencode console open}
     *
     * @return CLI 的退出状态、标准输出和错误输出
     */
    public OpenCodeCliResult consoleOpen() {
        return executor.execute("console", "open");
    }
}
