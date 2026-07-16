# opencode-java-sdk

纯 Java 库（无 Spring）：通过 HTTP REST API 和本地 CLI 与 [OpenCode](https://opencode.ai) Server 交互。

- **HTTP Server**：通过 `opencode serve` 启动的 REST API 进行会话管理、prompt 发送、agent 查询等
- **SSE 事件流**：消费 `GET /event` 实时事件
- **本地 CLI**：封装 `opencode run`、`opencode session` 等子命令

三条通道互不降级。入口类 [`OpenCodeClient`](src/main/java/io/github/hiwepy/opencode/OpenCodeClient.java)。

> 当前 SDK 适配 opencode **v1.17.18** CLI + Server HTTP API。
>
> 三个分支：
> - `feature/1.0.x` — JDK 1.8 兼容线（`1.0.x.20260630-SNAPSHOT`）
> - `main` / `feature/2.0.x` — JDK 17 兼容线（`2.0.x.20260630-SNAPSHOT`）
> - `feature/3.0.x` — JDK 21 兼容线（`3.0.x.20260630-SNAPSHOT`）

Spring Boot 应用请使用 [opencode-spring-boot-starter](../opencode-spring-boot-starter)。

## 快速开始

```java
OpenCodeClientConfig config = new OpenCodeClientConfig();
config.setServerUrl("http://localhost:4096");
config.setPassword("your-password"); // OPENCODE_SERVER_PASSWORD

OpenCodeClient client = new OpenCodeClient(config);

// 健康检查
HealthStatus health = client.health();
System.out.println("version: " + health.getVersion());

// 创建会话
Session session = client.createSession("my-task");

// 发送 prompt 并等待响应
PromptResult result = client.chatCompletion(session.getId(), "Explain how closures work in JavaScript");
System.out.println(result.getTextContent());

// 异步发送（不等待）
client.chatCompletionAsync(session.getId(), "Write a hello world in Python");

// 列出 agents
List<Agent> agents = client.listAgents();

client.close();
```

## HTTP Server API 映射

### Session / Prompt（核心）

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `health()` | `GET /global/health` | 健康检查 |
| `createSession(title)` | `POST /session` | 创建会话 |
| `getSession(id)` | `GET /session/:id` | 获取会话 |
| `listSessions()` | `GET /session` | 列出会话 |
| `listSessions(search, limit, start)` | `GET /session?...` | 分页/过滤 |
| `findSessionByTitle(title)` | `GET /session?search=` | 按 title 精确查找 |
| `deleteSession(id)` | `DELETE /session/:id` | 删除会话 |
| `getMessages(sessionId)` | `GET /session/:id/message` | 获取消息历史 |
| `getMessage(sessionId, messageId)` | `GET /session/:id/message/:messageID` | 获取单条 message |
| `chatCompletion(sessionId, request)` | `POST /session/:id/message` | 发送 prompt，同步等待 |
| `chatCompletionAsync(sessionId, request)` | `POST /session/:id/prompt_async` | 异步发送，不等待 |
| `abort(sessionId)` | `POST /session/:id/abort` | 中止会话 |
| `runSessionCommand(id, command, args, agent, model)` | `POST /session/:id/command` | 运行 slash command |
| `listAgents()` | `GET /agent` | 列出 agents |

### Session 扩展

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `getSessionStatusMap()` | `GET /session/status` | 所有 session 状态 |
| `getSessionChildren(id)` | `GET /session/:id/children` | 列出 forked sessions |
| `getSessionTodo(id)` | `GET /session/:id/todo` | session 任务列表 |
| `getSessionDiff(id, messageID?)` | `GET /session/:id/diff` | 文件 diff |
| `shareSession(id)` | `POST /session/:id/share` | 创建分享链接 |
| `unshareSession(id)` | `DELETE /session/:id/share` | 取消分享 |
| `forkSession(id, messageID?)` | `POST /session/:id/fork` | Fork session |
| `initSession(id, messageID, providerID, modelID)` | `POST /session/:id/init` | 用首个 message 初始化 |
| `summarizeSession(id, providerID, modelID)` | `POST /session/:id/summarize` | AI 摘要压缩 |
| `revertSession(id, messageID, partID?)` | `POST /session/:id/revert` | 回退到指定 message |
| `unrevertSession(id)` | `POST /session/:id/unrevert` | 撤销回退 |

### Config

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `getOpenCodeConfig()` | `GET /config` | 实例配置 |
| `getGlobalOpenCodeConfig()` | `GET /global/config` | 全局配置 |
| `updateOpenCodeConfig(body)` | `PATCH /config` | 更新实例配置 |
| `updateGlobalOpenCodeConfig(body)` | `PATCH /global/config` | 更新全局配置 |
| `getConfigProviders()` | `GET /config/providers` | 已配置的 providers + 默认模型 |

### Project

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `listProjects()` | `GET /project` | 所有项目 |
| `getCurrentProject()` | `GET /project/current` | 当前项目 |
| `updateProject(id, body)` | `PATCH /project/:id` | 更新项目元数据 |
| `initProjectGit()` | `POST /project/git/init` | 初始化 git 仓库 |

### Provider / Auth

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `listProviders()` | `GET /provider` | providers + defaults + connected |
| `listProviderAuthMethods()` | `GET /provider/auth` | 每个 provider 的认证方式 |
| `providerOAuthAuthorize(id, method)` | `POST /provider/:id/oauth/authorize` | 启动 OAuth 授权 |
| `providerOAuthCallback(id, code)` | `POST /provider/:id/oauth/callback` | 处理 OAuth 回调 |
| `setAuth(id, body)` | `PUT /auth/:id` | 设置 provider 凭证 |
| `removeAuth(id)` | `DELETE /auth/:id` | 清除 provider 凭证 |

### File / Find

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `listFiles(path)` | `GET /file?path=` | 列出文件/目录树 |
| `getFileContent(path)` | `GET /file/content?path=` | 读取文件内容 |
| `getFileStatus()` | `GET /file/status` | git 状态 |
| `find(pattern)` | `GET /find?pattern=` | ripgrep 文本搜索 |
| `findFiles(query)` | `GET /find/file?query=` | 按文件名查找 |
| `findSymbols(query)` | `GET /find/symbol?query=` | LSP 符号搜索 |

### Misc

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `listCommands()` | `GET /command` | slash commands |
| `listSkills()` | `GET /skill` | 已注册 skills |
| `listFormatters()` | `GET /formatter` | formatter 状态 |
| `listLsps()` | `GET /lsp` | LSP 状态 |
| `listMcpServers()` | `GET /mcp` | MCP 服务器状态 |
| `addMcpServer(name, config)` | `POST /mcp` | 动态添加 MCP |
| `getPath()` | `GET /path` | 工作目录相关路径 |
| `getVcs()` | `GET /vcs` | git 分支/脏标志 |
| `disposeInstance()` | `POST /instance/dispose` | 释放当前 instance |
| `globalDispose()` | `POST /global/dispose` | 释放所有 instance |
| `globalUpgrade(target?)` | `POST /global/upgrade` | 升级 opencode |

### Question / Permission

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `listQuestions()` | `GET /question` | 待回答问题 |
| `replyQuestion(id, answers)` | `POST /question/:id/reply` | 回复 |
| `rejectQuestion(id)` | `POST /question/:id/reject` | 拒绝 |
| `listPermissions()` | `GET /permission` | 待审批权限 |
| `replyPermission(id, response, remember)` | `POST /permission/:id/reply` | 回复权限请求 |

完整 API 文档：https://opencode.ai/docs/server/

## SSE 事件流

### 基础订阅

```java
OpenCodeSseClient sse = client.sse();
sse.subscribe(event -> {
    System.out.println("event: " + event.getType());
});

// 阻塞队列
BlockingQueue<Event> queue = sse.subscribeQueue();
Event event = queue.take();
```

### 类型化 EventHandler（推荐）

```java
client.onSessionEvent(sessionId, new EventHandler() {
    @Override public void onTextDelta(String delta, Event event) {
        System.out.print(delta);
    }
    @Override public void onToolCall(String name, Map<String, Object> input, Event event) {
        System.out.println("\ntool call: " + name);
    }
    @Override public void onSessionIdle(String sessionId, Event event) {
        System.out.println("\n[done]");
    }
});
```

### 事件类型过滤

```java
// 只关心 text.delta 和 session.idle
Set<String> types = new HashSet<>(Arrays.asList("message.part.updated", "session.idle"));
client.onEventTypes(types, event -> { /* ... */ });
```

## CLI 封装

### 核心子命令

```java
OpenCodeCli cli = client.cli();

// 非交互模式执行
OpenCodeCliResult result = cli.run("Explain async/await in JavaScript");
System.out.println(result.getStdout());

// 指定模型 / agent
cli.run("Hello", "anthropic/claude-sonnet-4-5");
cli.run("Hello", "plan", "anthropic/claude-sonnet-4-5");

// JSON 格式输出（流式事件）
cli.runJson("Hello");

// 会话 / agents / models
cli.sessionList();
cli.sessionDelete("session-id");
cli.agentList();
cli.models();
cli.models("anthropic", true, false);  // provider + verbose + refresh
```

### 服务端 / 升级 / 卸载

```java
cli.serve(4096, "127.0.0.1");         // opencode serve --port 4096 --hostname 127.0.0.1
cli.web(4096, "127.0.0.1");           // opencode web ...
cli.acp("/path/to/project");          // ACP server
cli.generate();                      // 输出 OpenAPI spec
cli.attach("http://localhost:4096", "/path", null, "user", "pass");
cli.upgrade();                       // 升级到最新
cli.upgrade("v1.18.0", "npm");       // 升级到指定版本
cli.uninstall(false, false, false, true);  // --force 跳过确认
```

### Provider / Auth / MCP

```java
cli.providersList();
cli.providersLogin("anthropic", "api-key");
cli.providersLogout("anthropic");

cli.mcpList();
cli.mcpAdd("context7", "https://mcp.context7.com/sse");
cli.mcpLogout("context7");
cli.mcpAuth("context7");
cli.mcpDebug("context7");
```

### Stats / Export / Import / DB / Debug

```java
cli.stats(7, 10, 5, "");            // --days 7 --tools 10 --models 5 --project current
cli.export("session-id", true);     // --sanitize
cli.importSession("https://opncd.ai/s/abc123");   // share URL
cli.db("SELECT count(*) FROM session;", "json");
cli.dbPath();

cli.debugConfig();
cli.debugPaths();
cli.debugInfo();
cli.debugScrap();
cli.debugSkill();
cli.debugStartup();
```

### GitHub / Plugin / Console

```java
cli.githubInstall();
cli.githubRun("issue_comment", "ghp_xxx");
cli.pr(123);

cli.plugin("opencode-anthropic-vertex", false, true);
cli.consoleLogin();
cli.consoleOrgs();
cli.consoleOpen();
```

所有 `cli*()` 方法在 `OpenCodeClient` 也有等价 facade 形式（如 `cliServe`、`cliModels` 等）。

## 配置

`OpenCodeClientConfig` 字段：

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `serverUrl` | `http://localhost:4096` | OpenCode Server 地址 |
| `username` | `opencode` | HTTP Basic Auth 用户名 |
| `password` | `null` | HTTP Basic Auth 密码（`OPENCODE_SERVER_PASSWORD`） |
| `connectTimeoutMillis` | `15000` | 连接超时（毫秒） |
| `readTimeoutMillis` | `300000` | 读超时（毫秒） |
| `verifySsl` | `true` | 是否校验 HTTPS 证书 |
| `defaultModel` | `null` | 默认模型（`provider/model`） |
| `defaultAgent` | `null` | 默认 agent |

`OpenCodeCliConfig` 字段：

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `executable` | `opencode` | CLI 可执行文件路径 |
| `timeout` | `300` | CLI 命令超时（秒） |
| `probeTimeoutSeconds` | `5` | CLI 可用性探测超时 |
| `workingDirectory` | `null` | CLI 子进程工作目录 |
| `maxConcurrentExecutions` | `0` | 最大并发子进程数（0 = 不限） |

## 认证

OpenCode Server 支持 HTTP Basic Auth，通过环境变量配置：

```bash
OPENCODE_SERVER_PASSWORD=your-password opencode serve
```

Java 端对应 `OpenCodeHttpClientConfig` 的 `username` 和 `password` 字段。

## 前置条件

1. 安装 OpenCode：`curl -fsSL https://opencode.ai/install | bash`
2. 启动 Server：`opencode serve --port 4096`
3. 配置 provider API key：`opencode auth login`

## 发布与 JDK

- 三条分支对应三档 JDK：
  - `feature/1.0.x` — JDK 1.8
  - `main` / `feature/2.0.x` — JDK 17
  - `feature/3.0.x` — JDK 21
- 发布快照/正式版：

```bash
mvn clean deploy -DskipTests
```

发布到阿里云 Maven 仓库（`2624322-snapshot-3EoOv3` / `2624322-release-6F6h6R`），详细见 `pom.xml` 的 `distributionManagement`。