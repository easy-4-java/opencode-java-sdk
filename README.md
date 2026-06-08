# opencode-java-sdk

纯 Java 库（无 Spring）：通过 HTTP REST API、SSE 事件流和本地 CLI 与 [OpenCode](https://opencode.ai) Server **完整对接**。

- **HTTP Server**：覆盖 `opencode serve` 的所有 REST API（全局、会话、消息、权限、问答、Provider、配置、项目管理、PTY、VCS、MCP、文件查找、工作区、TUI 控制等 120+ 端点）
- **SSE 事件流**：支持 V1 `/event`、全局 `/global/event`、V2 `/api/event` 三路事件流，含类型化事件分发
- **本地 CLI**：封装 `opencode` 全部 24 个顶层命令

三条通道互不降级。入口类 [`OpenCodeClient`](src/main/java/io/github/hiwepy/opencode/OpenCodeClient.java)。

Spring Boot 应用请使用 [opencode-spring-boot-starter](../opencode-spring-boot-starter)。

## 快速开始

```java
OpenCodeClientConfig config = new OpenCodeClientConfig();
config.setServerUrl("http://localhost:4096");
config.setPassword("your-password"); // OPENCODE_SERVER_PASSWORD

OpenCodeClient client = new OpenCodeClient(config);

// 健康检查
HealthStatus health = client.health();

// 创建会话
Session session = client.createSession("my-task");

// 发送 prompt 并等待响应
PromptResult result = client.prompt(session.getId(), "Explain closures in JavaScript");
System.out.println(result.getTextContent());

// 完整的 prompt 请求（支持 model、agent、tools、format 等）
PromptRequest req = PromptRequest.ofText("Hello", "anthropic", "claude-sonnet-4-5");
client.prompt(session.getId(), req);

// Fork 会话
Session forked = client.forkSession(session.getId());

// 摘要会话
client.summarizeSession(session.getId());

// 分享会话
ShareInfo share = client.shareSession(session.getId());

client.close();
```

## HTTP Server API 完整映射

### 全局操作
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `health()` | `GET /global/health` | 健康检查 |
| `dispose()` | `POST /global/dispose` | 销毁所有实例 |
| `upgrade()` | `POST /global/upgrade` | 升级 opencode |
| `getGlobalConfig()` | `GET /global/config` | 获取全局配置 |
| `updateGlobalConfig(cfg)` | `PATCH /global/config` | 更新全局配置 |

### 项目配置
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `getProjectConfig()` | `GET /config` | 获取项目配置 |
| `updateProjectConfig(cfg)` | `PATCH /config` | 更新项目配置 |
| `getConfigProviders()` | `GET /config/providers` | 列出已配置的 provider |

### 会话管理
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `createSession(title)` | `POST /session` | 创建会话 |
| `createSession(SessionCreateRequest)` | `POST /session` | 创建会话（完整参数） |
| `getSession(id)` | `GET /session/:id` | 获取会话 |
| `updateSession(id, req)` | `PATCH /session/:id` | 更新会话 |
| `listSessions()` | `GET /session` | 列出会话 |
| `deleteSession(id)` | `DELETE /session/:id` | 删除会话 |
| `getSessionStatus()` | `GET /session/status` | 获取会话状态 |
| `getSessionChildren(id)` | `GET /session/:id/children` | 列出 fork 子会话 |
| `getSessionTodos(id)` | `GET /session/:id/todo` | 获取 TODO 列表 |
| `getSessionDiff(id)` | `GET /session/:id/diff` | 获取消息 diff |
| `forkSession(id)` | `POST /session/:id/fork` | Fork 会话 |
| `initSession(id)` | `POST /session/:id/init` | 初始化会话（AGENTS.md） |
| `shareSession(id)` | `POST /session/:id/share` | 创建分享链接 |
| `unshareSession(id)` | `DELETE /session/:id/share` | 删除分享链接 |
| `summarizeSession(id)` | `POST /session/:id/summarize` | 摘要/压缩会话 |

### 消息/Prompt
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `prompt(sessionId, request)` | `POST /session/:id/message` | 发送 prompt，同步等待 |
| `promptAsync(sessionId, request)` | `POST /session/:id/prompt_async` | 异步发送，不等待 |
| `getMessages(sessionId)` | `GET /session/:id/message` | 获取消息历史 |
| `getMessage(sessionId, msgId)` | `GET /session/:id/message/:mid` | 获取指定消息 |
| `deleteMessage(sessionId, msgId)` | `DELETE /session/:id/message/:mid` | 删除消息 |
| `abort(sessionId)` | `POST /session/:id/abort` | 中止会话 |
| `revertMessage(sessionId)` | `POST /session/:id/revert` | 回滚消息 |
| `unrevertMessage(sessionId)` | `POST /session/:id/unrevert` | 恢复回滚的消息 |
| `deletePart(sid, mid, pid)` | `DELETE .../part/:pid` | 删除消息部件 |
| `updatePart(sid, mid, pid, body)` | `PATCH .../part/:pid` | 更新消息部件 |

### 会话命令
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `executeCommand(sessionId, cmd)` | `POST /session/:id/command` | 在会话中执行命令 |
| `runShell(sessionId, request)` | `POST /session/:id/shell` | 运行 shell 命令 |

### 权限管理
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listPermissions()` | `GET /permission` | 列出待处理权限请求 |
| `replyToPermission(id, reply)` | `POST /permission/:id/reply` | 回复权限请求 |
| `allowPermission(id)` | — | 快捷：允许一次 |
| `allowPermissionAlways(id)` | — | 快捷：始终允许 |
| `rejectPermission(id, msg)` | — | 快捷：拒绝 |

### 问答系统
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listQuestions()` | `GET /question` | 列出待处理问题 |
| `replyToQuestion(id, reply)` | `POST /question/:id/reply` | 回答问题 |
| `answerQuestion(id, answer)` | — | 快捷：回答 |
| `rejectQuestion(id)` | `POST /question/:id/reject` | 拒绝问题 |

### Provider & Auth
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listProviders()` | `GET /provider` | 列出 AI provider |
| `getProviderAuth()` | `GET /provider/auth` | 获取认证方式 |
| `setAuth(providerId, cred)` | `PUT /auth/:providerID` | 设置认证凭据 |
| `removeAuth(providerId)` | `DELETE /auth/:providerID` | 移除认证凭据 |
| `startOAuth(providerId)` | `POST /provider/:id/oauth/authorize` | 启动 OAuth |
| `completeOAuth(providerId, params)` | `POST /provider/:id/oauth/callback` | 完成 OAuth |

### Agent / Command / Skill
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listAgents()` | `GET /agent` | 列出 agents |
| `listCommands()` | `GET /command` | 列出命令 |
| `listSkills()` | `GET /skill` | 列出技能 |

### 文件与查找
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listFiles(directory)` | `GET /file` | 列出目录文件 |
| `getFileContent(path)` | `GET /file/content` | 读取文件内容 |
| `getFileStatus(path)` | `GET /file/status` | 获取文件 Git 状态 |
| `find(query)` | `GET /find` | 搜索文本 |
| `findFile(pattern)` | `GET /find/file` | 按文件名搜索 |
| `findSymbol(query)` | `GET /find/symbol` | 搜索 LSP 符号 |
| `getFormatter()` | `GET /formatter` | 获取格式化器状态 |
| `getLspStatus()` | `GET /lsp` | 获取 LSP 状态 |
| `getPathInfo()` | `GET /path` | 获取工作目录信息 |

### 项目管理
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listProjects()` | `GET /project` | 列出项目 |
| `getCurrentProject()` | `GET /project/current` | 获取当前项目 |
| `initGitProject()` | `POST /project/git/init` | 初始化 Git 仓库 |
| `updateProject(id, update)` | `PATCH /project/:id` | 更新项目 |
| `listProjectDirectories(id)` | `GET /project/:id/directories` | 列出项目目录 |

### PTY 终端
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listPtys()` | `GET /pty` | 列出 PTY 会话 |
| `createPty(params)` | `POST /pty` | 创建 PTY |
| `getPtyShells()` | `GET /pty/shells` | 列出可用 shell |
| `getPty(id)` | `GET /pty/:id` | 获取 PTY |
| `updatePty(id, params)` | `PUT /pty/:id` | 更新 PTY |
| `deletePty(id)` | `DELETE /pty/:id` | 删除 PTY |
| `createPtyConnectToken(id)` | `POST /pty/:id/connect-token` | 创建连接令牌 |

### VCS（Git）
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `getVcsInfo()` | `GET /vcs` | 获取 VCS 信息 |
| `getVcsStatus()` | `GET /vcs/status` | 获取 Git 状态 |
| `getVcsDiff()` | `GET /vcs/diff` | 获取 diff |
| `getVcsDiffRaw()` | `GET /vcs/diff/raw` | 获取原始 patch |
| `applyPatch(request)` | `POST /vcs/apply` | 应用补丁 |

### MCP 管理
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `getMcpStatus()` | `GET /mcp` | 获取 MCP 状态 |
| `addMcpServer(config)` | `POST /mcp` | 动态添加 MCP 服务器 |
| `connectMcpServer(name)` | `POST /mcp/:name/connect` | 连接 MCP |
| `disconnectMcpServer(name)` | `POST /mcp/:name/disconnect` | 断开 MCP |
| `removeMcpAuth(name)` | `DELETE /mcp/:name/auth` | 移除 MCP OAuth |
| `startMcpOAuth(name)` | `POST /mcp/:name/auth` | 启动 MCP OAuth |
| `completeMcpOAuth(name, params)` | `POST /mcp/:name/auth/callback` | 完成 MCP OAuth |
| `authenticateMcp(name)` | `POST /mcp/:name/auth/authenticate` | MCP 认证 |

### 实验性功能
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listTools()` | `GET /experimental/tool` | 列出工具 |
| `listToolIds()` | `GET /experimental/tool/ids` | 列出工具 ID |
| `listWorktrees()` | `GET /experimental/worktree` | 列出 worktree |
| `createWorktree(params)` | `POST /experimental/worktree` | 创建 worktree |
| `deleteWorktree(params)` | `DELETE /experimental/worktree` | 删除 worktree |
| `resetWorktree(name)` | `POST /experimental/worktree/reset` | 重置 worktree |
| `listSessionsV2(params)` | `GET /experimental/session` | V2 式列出会话 |
| `listResources()` | `GET /experimental/resource` | 列出 MCP 资源 |

### 工作区
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `listWorkspaces()` | `GET /experimental/workspace` | 列出工作区 |
| `createWorkspace(params)` | `POST /experimental/workspace` | 创建工作区 |
| `deleteWorkspace(id)` | `DELETE /experimental/workspace/:id` | 删除工作区 |
| `warpToWorkspace(sid, wid)` | `POST /experimental/workspace/warp` | 转移会话到工作区 |

### TUI 控制
| 方法 | HTTP API |
|------|----------|
| `tuiAppendPrompt(p)` / `tuiSubmitPrompt(p)` / `tuiClearPrompt()` | TUI prompt 操作 |
| `tuiOpenHelp()` / `tuiOpenSessions()` / `tuiOpenThemes()` / `tuiOpenModels()` | TUI 导航 |
| `tuiExecuteCommand(cmd)` / `tuiShowToast(msg, type)` / `tuiPublish(evt, data)` | TUI 交互 |
| `tuiSelectSession(id)` | TUI 导航到会话 |

### V2 API
| 方法 | HTTP API | 说明 |
|------|----------|------|
| `healthV2()` | `GET /api/health` | V2 健康检查 |
| `listAgentsV2()` | `GET /api/agent` | V2 agent 列表 |
| `listSessionsApi(params)` | `GET /api/session` | V2 会话列表（分页） |
| `listModelsApi()` | `GET /api/model` | V2 模型列表 |
| `listProvidersApi()` | `GET /api/provider` | V2 provider 列表 |
| `getProviderApi(id)` | `GET /api/provider/:id` | V2 provider 详情 |

## SSE 事件流

```java
OpenCodeSseClient sse = client.sse();

// V1 事件流（/event）
sse.subscribe(event -> System.out.println(event.getType()));

// 全局事件流（/global/event）
client.subscribeGlobalEvents(event -> ...);

// V2 事件流（/api/event）
client.subscribeV2Events(event -> ...);

// 阻塞队列方式
BlockingQueue<Event> queue = client.subscribeEventQueue();

// 类型化事件分发
client.subscribeTypedEvents(new OpenCodeSseClient.TypedEventHandler() {
    public void onTextDelta(TypedEvent e) { /* session.next.text.delta */ }
    public void onTool(TypedEvent e)      { /* tool.called/success/failed */ }
    public void onStep(TypedEvent e)      { /* step.started/ended/failed */ }
    public void onShell(TypedEvent e)     { /* shell.started/ended */ }
    public void onAny(TypedEvent e)       { /* 所有事件 */ }
});

// 内置事件类型判断
EventParser.isTextDelta(event);
EventParser.isToolEvent(event);
EventParser.isStepEvent(event);
EventParser.isReasoningDelta(event);
```

## CLI 封装

```java
OpenCodeCli cli = client.cli();

// run — 核心命令
cli.run("Explain async/await");
cli.run("Hello", "anthropic/claude-sonnet-4-5");
cli.run("Hello", "my-agent", "anthropic/claude-sonnet-4-5");
cli.runJson("Hello");

// 会话管理
cli.sessionList();
cli.sessionDelete("session-id");

// Agent & Model
cli.agentList();
cli.agentCreate("my-agent");
cli.models();
cli.models("anthropic");

// Provider 管理
cli.providersList();
cli.providersLogin();
cli.providersLogin("anthropic");
cli.providersLogout();
cli.authList();

// MCP 管理
cli.mcpList();
cli.mcpAdd("--name", "my-server", "--command", "npx", "-y", "@my/mcp");
cli.mcpAuth("my-server");
cli.mcpLogout("my-server");
cli.mcpDebug();

// 服务器控制
cli.serve();
cli.serve(4096);
cli.web();

// 账号
cli.consoleLogin();
cli.consoleLogout();
cli.consoleSwitch();
cli.consoleOrgs();
cli.consoleOpen();

// TUI
cli.thread();
cli.attach();

// 运维
cli.stats();
cli.upgrade();
cli.uninstall();

// 数据
cli.exportSession();
cli.exportSession("session-id");
cli.importSession("file.json");
cli.dbQuery("SELECT * FROM sessions");
cli.dbPath();

// GitHub
cli.githubInstall();
cli.githubRun();
cli.pr("42");

// 其他
cli.generate();
cli.debug();
cli.plugin("my-plugin");
cli.completion("bash");
cli.acp();
cli.version();
cli.help();
```

## 配置

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `serverUrl` | `http://localhost:4096` | OpenCode Server 地址 |
| `username` | `opencode` | HTTP Basic Auth 用户名 |
| `password` | `null` | HTTP Basic Auth 密码 |
| `connectTimeoutMillis` | `15000` | 连接超时（毫秒） |
| `readTimeoutMillis` | `300000` | 读超时（毫秒） |
| `verifySsl` | `true` | 校验 HTTPS 证书 |
| `localExecutable` | `opencode` | CLI 可执行文件路径 |
| `localTimeoutSeconds` | `300` | CLI 命令超时（秒） |
| `defaultModel` | `null` | 默认模型 |
| `defaultAgent` | `null` | 默认 agent |

## 包结构

```
io.github.hiwepy.opencode
├── OpenCodeClient              # 门面（所有 API 入口）
├── OpenCodeClientConfig        # 配置 POJO
├── cli/
│   ├── OpenCodeCli             # 24 个 CLI 命令封装
│   ├── OpenCodeCliExecutor     # Commons Exec 子进程执行器
│   └── OpenCodeCliResult       # CLI 执行结果
├── http/
│   ├── OpenCodeHttpClient      # HTTP REST 客户端（120+ 端点）
│   └── OpenCodeSseClient       # SSE 事件流客户端（3 路 + 类型化）
├── mapper/
│   └── OpenCodeCallbackParser  # AI 响应 JSON 解析
├── model/                      # 30+ 数据模型
│   ├── event/                  # 类型化 SSE 事件
│   │   ├── TypedEvent          # 事件基类
│   │   ├── TextDeltaEvent      # 文本增量
│   │   ├── StepEvent           # 步骤事件
│   │   ├── ToolEvent           # 工具事件
│   │   ├── ReasoningDeltaEvent # 推理增量
│   │   ├── ShellEvent          # Shell 事件
│   │   ├── LifecycleEvent      # 生命周期事件
│   │   └── EventParser         # 事件类型解析
│   └── ...                     # 30+ 请求/响应模型
├── exception/
│   ├── OpenCodeException       # SDK 异常基类
│   └── OpenCodeHttpException   # HTTP 异常（含状态码）
└── util/
```

## 前置条件

1. 安装 OpenCode：`curl -fsSL https://opencode.ai/install | bash`
2. 启动 Server：`opencode serve --port 4096`
3. 配置 provider API key：`opencode auth login`

## 发布与 JDK

- 本模块要求 **JDK 17**
- 发布快照/正式版：

```bash
mvn clean deploy -DskipTests
```
