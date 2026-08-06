# opencode-java-sdk

<div align="center">

**纯 Java 库（无 Spring）：通过 HTTP REST API、SSE 事件流与本地 CLI 与 OpenCode Server 交互**

![Java](https://img.shields.io/badge/Java-8-orange) ![License](https://img.shields.io/badge/license-Apache%202.0-green)

[English](./README.md) | [简体中文](./README.zh-CN.md)

[1. 项目概述](#1-project-overview) · [2. 能力与状态](#2-features--status) · [3. 运行要求与兼容性](#3-requirements--compatibility) · [4. 架构与模块](#4-architecture--modules) · [5. 引入依赖](#5-installation) · [6. 快速开始](#6-quick-start) · [7. 配置](#7-configuration) · [8. 核心用法](#8-core-usage) · [9. 测试与构建](#9-testing--build) · [10. 版本线与分支](#10-versioning--branches) · [11. 贡献与许可证](#11-contributing--license)

</div>

---

> **当前分支**：`feature/1.0.x`<br>
> **版本**：`1.0.x.20260630-SNAPSHOT`<br>
> **JDK 基线**：8<br>
> **项目状态**：稳定（1.0.x 线）。尚未发布 Maven Central；制品通过 Aliyun Maven 仓库与 GitHub Releases 分发。

<a id="1-project-overview"></a>
## 1. 项目概述

### 1.1 是什么

**opencode-java-sdk** 是纯 Java 库（无 Spring），通过三条互不降级的通道与 [OpenCode](https://opencode.ai) Server 交互：

- **HTTP Server API**——通过 `opencode serve` 暴露的 REST API 进行会话管理、prompt 发送、agent 查询以及 config / project / provider / file 管理；
- **SSE 事件流**——消费 `GET /event` 实时事件；
- **本地 CLI**——封装 `opencode run`、`opencode session` 等大量子命令。

当前 SDK 适配 opencode **v1.17.18** CLI + Server HTTP API。

### 1.2 不是什么

- 不是 OpenCode Server 本身。
- 无 Spring 依赖；Spring Boot 应用请使用配套的 `opencode-spring-boot-starter`。

### 1.3 典型使用场景

| 场景 | 推荐入口 | 结果 |
|---|---|---|
| 健康检查 | `client.health()` | Server 版本信息 |
| 创建会话并提问 | `client.createSession(title)` + `client.chatCompletion(id, text)` | `PromptResult.getTextContent()` |
| 异步发送（不等待） | `client.chatCompletionAsync(...)` | 立即返回 |
| 实时事件消费 | `client.sse().subscribe(...)` / `client.onSessionEvent(...)` | 类型化 text-delta / tool-call / idle 回调 |
| 会话管理 | `client.listSessions()` / `deleteSession()` / `shareSession()` ... | 完整会话 CRUD + share / fork / revert |
| 文件与查找 | `client.listFiles(path)` / `find(pattern)` / `findFiles(query)` | 文件树、ripgrep、文件名与符号搜索 |
| 本地 CLI 自动化 | `client.cli().run(...)` / `sessionList()` / `serve(...)` ... | `OpenCodeCliResult` |

<a id="2-features--status"></a>
## 2. 能力与状态

| 能力 | 状态 | 说明 |
|---|:---:|---|
| 会话与 prompt HTTP API | 可用 | 创建 / 获取 / 列表 / 删除，同步 + 异步对话、中止、slash command |
| 会话扩展 | 可用 | 状态表、子会话、todo、diff、share、fork、init、summarize、revert / unrevert |
| Config API | 可用 | 实例与全局配置读写、providers + 默认模型 |
| Project API | 可用 | 列表 / 当前 / 更新、git init |
| Provider 与认证 API | 可用 | providers、认证方式、OAuth authorize / callback、set / remove auth |
| File / find API | 可用 | 文件树 + 内容 + git 状态、ripgrep / 文件名 / 符号搜索 |
| 其他 API | 可用 | commands、skills、formatters、LSPs、MCP servers、path、VCS、instance dispose、global upgrade |
| Question / permission API | 可用 | 待回答问题与权限、reply / reject |
| SSE 事件流 | 可用 | `subscribe`、`subscribeQueue`、`subscribeSession`、`subscribeEventTypes`、类型化 `EventHandler` |
| CLI 封装 | 可用 | `run`、`runJson`、sessions、agents、models、providers/auth、MCP、stats、export / import、db、debug、serve / web / attach、github、plugin、console |

<a id="3-requirements--compatibility"></a>
## 3. 运行要求与兼容性

| 组件 | 版本 | 说明 |
|---|---:|---|
| JDK | 8+ | 1.0.x 线基线 |
| Maven | 3.0+ | Enforcer 下限 |
| OkHttp / okhttp-sse | 4.12.0 | HTTP 与 SSE 传输 |
| Jackson databind | 2.17.x | JSON |
| commons-exec | — | CLI 子进程执行 |
| MockWebServer | test scope | HTTP 客户端测试 |

版本线矩阵：

| 版本线 | 分支 | JDK | 版本模式 | 用途 |
|---|---|---:|---|---|
| 1.0.x | `feature/1.0.x`（当前分支） | 8 | `1.0.x.*` | 存量项目、Boot 2.x Starter 线 |
| 2.0.x | `feature/2.0.x` | 17 | `2.0.x.*` | 主流线（JDK 17） |
| 3.0.x | `feature/3.0.x` | 21 | `3.0.x.*` | 新项目 |

<a id="4-architecture--modules"></a>
## 4. 架构与模块

```text
[ Java 应用 ]
        |
        | opencode-java-sdk
        v
+------------------------------------------+
| OpenCodeClient（门面）                    |
|  HTTP   /session、/agent、/config、       |
|         /project、/provider、/file、      |
|         /find、/command、/skill、/mcp ... |
|  SSE    GET /event（subscribe、类型化     |
|         EventHandler 回调）               |
|  CLI    opencode run / session / serve /  |
|         auth / mcp / debug ...            |
+------------------------------------------+
        |
        v
[ OpenCode Server（opencode serve）]
```

单模块库（打包类型 `jar`）。包结构：

| 包 | 职责 |
|---|---|
| `io.github.easy4j.opencode` | 门面 `OpenCodeClient` 与配置类 |
| `io.github.easy4j.opencode.api` | `OpenCodeHttpClient`、`OpenCodeSseClient`、`OpenCodeRequestContext` |
| `io.github.easy4j.opencode.api.model` | DTO（`Session`、`PromptRequest`、`PromptResult`、`Agent`、`Project` 等） |
| `io.github.easy4j.opencode.api.event` | 类型化 `EventHandler` |
| `io.github.easy4j.opencode.api.mapper` | 消息映射与回调解析 |
| `io.github.easy4j.opencode.cli` | CLI 门面（`OpenCodeCli` / `OpenCodeCliExecutor`） |
| `io.github.easy4j.opencode.cli.availability` | CLI 可用性探测 |
| `io.github.easy4j.opencode.exception` | 异常层级 |

<a id="5-installation"></a>
## 5. 引入依赖

Maven：

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>opencode-java-sdk</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

Gradle：

```groovy
implementation 'io.github.easy4j:opencode-java-sdk:1.0.x.20260630-SNAPSHOT'
```

快照版本需要启用对应快照仓库（`pom.xml` 中 `distributionManagement` 指向 Aliyun Maven 仓库）。

<a id="6-quick-start"></a>
## 6. 快速开始

```java
OpenCodeClientConfig config = new OpenCodeClientConfig();
config.getHttp().setServerUrl("http://localhost:4096");
config.getHttp().setPassword("your-password");   // 与 OPENCODE_SERVER_PASSWORD 对应

OpenCodeClient client = new OpenCodeClient(config);

// 健康检查
HealthStatus health = client.health();
System.out.println("version: " + health.getVersion());

// 创建会话并对话
Session session = client.createSession("my-task");
PromptResult result = client.chatCompletion(session.getId(),
        "Explain how closures work in JavaScript");
System.out.println(result.getTextContent());

// 异步发送（不等待）
client.chatCompletionAsync(session.getId(), "Write a hello world in Python");

// 列出 agents
List<Agent> agents = client.listAgents();

client.close();
```

**预期结果**：在 4096 端口运行 `opencode serve`（以 `OPENCODE_SERVER_PASSWORD=your-password opencode serve` 启动）的前提下，`health()` 返回服务端版本，对话调用返回模型文本内容，异步 prompt 被接受且不阻塞。

<a id="7-configuration"></a>
## 7. 配置

配置为对象式。`OpenCodeClientConfig` 聚合 `http`（`OpenCodeHttpClientConfig`）与 `cli`（`OpenCodeCliConfig`）。

`OpenCodeHttpClientConfig` 属性：

| 属性 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 是否启用 HTTP 子系统 |
| `startupCheckEnabled` | `false` | 启动时探测 Server |
| `failFastOnUnavailable` | `false` | 探测失败时中断构造 |
| `serverUrl` | `http://localhost:4096` | OpenCode Server 地址 |
| `username` | `opencode` | HTTP Basic Auth 用户名 |
| `password` | `null` | HTTP Basic Auth 密码（`OPENCODE_SERVER_PASSWORD`） |
| `connectTimeoutMillis` | `15000` | 连接超时（毫秒） |
| `readTimeoutMillis` | `300000` | 读取超时（毫秒） |
| `verifySsl` | `true` | 是否校验 HTTPS 证书 |
| `defaultModel` | `null` | 默认模型（`provider/model`） |
| `defaultAgent` | `null` | 默认 agent |

`OpenCodeCliConfig` 属性：

| 属性 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 是否启用 CLI 子系统 |
| `startupCheckEnabled` | `false` | 启动时探测 `opencode --version` |
| `failFastOnUnavailable` | `false` | 探测失败时中断构造 |
| `executable` | `opencode` | 可执行文件名或绝对路径 |
| `timeout` | `300` | CLI 命令超时（秒） |
| `probeTimeoutSeconds` | `5` | 可用性探测超时（秒） |
| `workingDirectory` | `null` | 子进程工作目录 |
| `maxConcurrentExecutions` | `0` | 最大并发子进程数（0 = 不限） |

<a id="8-core-usage"></a>
## 8. 核心用法

### 8.1 SSE 事件与类型化 EventHandler

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

### 8.2 CLI 自动化

```java
OpenCodeCli cli = client.cli();

// 非交互模式执行
OpenCodeCliResult result = cli.run("Explain async/await in JavaScript");
System.out.println(result.getStdout());

cli.run("Hello", "plan", "anthropic/claude-sonnet-4-5");  // agent + model
cli.sessionList();
cli.serve(4096, "127.0.0.1");                             // opencode serve --port 4096 --hostname 127.0.0.1
cli.upgrade("v1.18.0", "npm");
```

所有 `cli*()` 方法在 `OpenCodeClient` 上也有等价 facade 形式（如 `cliServe`、`cliModels`）。

<a id="9-testing--build"></a>
## 9. 测试与构建

```bash
mvn clean verify
```

- HTTP 客户端使用 MockWebServer 测试（`src/test/java/io/github/easy4j/opencode/api/OpenCodeHttpClientTest.java`）。
- JaCoCo 在 `verify` 阶段执行 `prepare-agent`、`report` 与 `check`，行覆盖率规则为 **90%**（`haltOnFailure=false`）。
- 发布打包（`mvn -Prelease deploy`）附带 sources 与 javadoc 构件并执行 GPG 签名，对接 Sonatype Central Publishing；普通 `mvn deploy` 按版本后缀路由到 Aliyun Maven 仓库（见 `distributionManagement`）。

<a id="10-versioning--branches"></a>
## 10. 版本线与分支

| 分支 | 版本模式 | JDK | 维护策略 |
|---|---|---|---|
| `feature/1.0.x`（当前分支） | `1.0.x.*` | 8 | 仅接受兼容性修复与 JDK 8 安全的依赖升级 |
| `feature/2.0.x` | `2.0.x.*` | 17 | 主流开发线 |
| `feature/3.0.x` | `3.0.x.*` | 21 | 新项目 |

<a id="11-contributing--license"></a>
## 11. 贡献与许可证

提交 Pull Request 前请执行 `mvn clean verify`，并说明兼容性、测试、文档与迁移影响。本项目采用 [Apache License 2.0](LICENSE) 许可证。
