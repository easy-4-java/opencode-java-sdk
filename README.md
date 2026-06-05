# opencode-java-sdk

纯 Java 库（无 Spring）：通过 HTTP REST API 和本地 CLI 与 [OpenCode](https://opencode.ai) Server 交互。

- **HTTP Server**：通过 `opencode serve` 启动的 REST API 进行会话管理、prompt 发送、agent 查询等
- **SSE 事件流**：消费 `GET /event` 实时事件
- **本地 CLI**：封装 `opencode run`、`opencode session` 等子命令

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
System.out.println("version: " + health.getVersion());

// 创建会话
Session session = client.createSession("my-task");

// 发送 prompt 并等待响应
PromptResult result = client.prompt(session.getId(), "Explain how closures work in JavaScript");
System.out.println(result.getTextContent());

// 异步发送（不等待）
client.promptAsync(session.getId(), "Write a hello world in Python");

// 列出 agents
List<Agent> agents = client.listAgents();

client.close();
```

## HTTP Server API 映射

| Java 方法 | HTTP API | 说明 |
|-----------|----------|------|
| `health()` | `GET /global/health` | 健康检查 |
| `createSession(title)` | `POST /session` | 创建会话 |
| `getSession(id)` | `GET /session/:id` | 获取会话 |
| `listSessions()` | `GET /session` | 列出会话 |
| `deleteSession(id)` | `DELETE /session/:id` | 删除会话 |
| `prompt(sessionId, request)` | `POST /session/:id/message` | 发送 prompt，同步等待 |
| `promptAsync(sessionId, request)` | `POST /session/:id/prompt_async` | 异步发送，不等待 |
| `getMessages(sessionId)` | `GET /session/:id/message` | 获取消息历史 |
| `abort(sessionId)` | `POST /session/:id/abort` | 中止会话 |
| `listAgents()` | `GET /agent` | 列出 agents |

完整 API 文档：https://opencode.ai/docs/server/

## SSE 事件流

```java
OpenCodeSseClient sse = client.sse();
sse.subscribe(event -> {
    System.out.println("event: " + event.getType());
});

// 或使用阻塞队列
BlockingQueue<Event> queue = sse.subscribeQueue();
Event event = queue.take();
```

## CLI 封装

```java
OpenCodeCli cli = client.cli();

// 非交互模式执行
OpenCodeCliResult result = cli.run("Explain async/await in JavaScript");
System.out.println(result.getStdout());

// 指定模型
cli.run("Hello", "anthropic/claude-sonnet-4-5");

// JSON 格式输出
OpenCodeCliResult jsonResult = cli.runJson("Hello");

// 会话管理
cli.sessionList();
cli.sessionDelete("session-id");

// 其他命令
cli.agentList();
cli.models();
cli.mcpList();
cli.authList();
```

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
| `localExecutable` | `opencode` | CLI 可执行文件路径 |
| `localTimeoutSeconds` | `300` | CLI 命令超时（秒） |
| `defaultModel` | `null` | 默认模型（`provider/model`） |
| `defaultAgent` | `null` | 默认 agent |

## 认证

OpenCode Server 支持 HTTP Basic Auth，通过环境变量配置：

```bash
OPENCODE_SERVER_PASSWORD=your-password opencode serve
```

Java 端对应 `OpenCodeClientConfig` 的 `username` 和 `password` 字段。

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
