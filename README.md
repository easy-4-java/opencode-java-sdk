# opencode-java-sdk

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-21-orange)](https://github.com/easy-4-java/opencode-java-sdk) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

Pure Java SDK (no Spring) for the OpenCode Server: HTTP REST API, SSE event stream and local CLI
[简体中文](./README.zh-CN.md)

> **Current branch**: `feature/3.0.x`
> **Version**: `3.0.x.x.20260630-SNAPSHOT`
> **JDK baseline**: 8
> **Project status**: stable (1.0.x line). Not yet published to Maven Central; artifacts are distributed via the Aliyun Maven repository and GitHub Releases.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage](#8-core-usage)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

### 1.1 What it is

**opencode-java-sdk** is a pure Java library (no Spring) for interacting with the [OpenCode](https://opencode.ai) Server through three independent, non-degrading channels:

- **HTTP Server API** — session management, prompt sending, agent queries, config/project/provider/file management over the REST API exposed by `opencode serve`;
- **SSE event stream** — consuming `GET /event` real-time events;
- **Local CLI** — wrapping `opencode run`, `opencode session` and many more subcommands.

The current SDK adapts opencode **v1.17.18** CLI + Server HTTP API.

### 1.2 What it is not

- Not the OpenCode Server itself.
- No Spring dependency; Spring Boot applications should use the companion `opencode-spring-boot-starter`.

### 1.3 Typical scenarios

| Scenario | Recommended entry | Result |
|---|---|---|
| Health check | `client.health()` | Server version info |
| Create a session and ask a prompt | `client.createSession(title)` + `client.chatCompletion(id, text)` | `PromptResult.getTextContent()` |
| Fire-and-forget prompt | `client.chatCompletionAsync(...)` | No waiting |
| Real-time event consumption | `client.sse().subscribe(...)` / `client.onSessionEvent(...)` | Typed text-delta / tool-call / idle callbacks |
| Session management | `client.listSessions()` / `deleteSession()` / `shareSession()` ... | Full session CRUD + share/fork/revert |
| File & find | `client.listFiles(path)` / `find(pattern)` / `findFiles(query)` | File tree, ripgrep, filename and symbol search |
| Local CLI automation | `client.cli().run(...)` / `sessionList()` / `serve(...)` ... | `OpenCodeCliResult` |

<a id="2-features--status"></a>
## 2. Features & Status

| Capability | Status | Notes |
|---|:---:|---|
| Session & prompt HTTP API | Available | create/get/list/delete, sync + async chat, abort, slash commands |
| Session extensions | Available | status map, children, todo, diff, share, fork, init, summarize, revert/unrevert |
| Config API | Available | instance + global config get/update, providers + defaults |
| Project API | Available | list/current/update, git init |
| Provider & auth API | Available | providers, auth methods, OAuth authorize/callback, set/remove auth |
| File / find API | Available | file tree + content + git status, ripgrep / filename / symbol search |
| Misc API | Available | commands, skills, formatters, LSPs, MCP servers, path, VCS, instance dispose, global upgrade |
| Question / permission API | Available | pending questions + permissions, reply/reject |
| SSE event stream | Available | `subscribe`, `subscribeQueue`, `subscribeSession`, `subscribeEventTypes`, typed `EventHandler` |
| CLI wrapper | Available | `run`, `runJson`, sessions, agents, models, providers/auth, MCP, stats, export/import, db, debug, serve/web/attach, github, plugin, console |

<a id="3-requirements--compatibility"></a>
## 3. Requirements & Compatibility

| Component | Version | Notes |
|---|---:|---|
| JDK | 21+ | 1.0.x line baseline |
| Maven | 3.0+ | Enforcer minimum |
| OkHttp / okhttp-sse | 4.12.0 | HTTP + SSE transport |
| Jackson databind | 2.17.x | JSON |
| commons-exec | — | CLI subprocess execution |
| MockWebServer | test scope | HTTP client tests |

Version-line matrix:

| Version line | Branch | JDK | Version pattern | Purpose |
|---|---|---:|---|---|
| 1.0.x | `feature/3.0.x` (this branch) | 8 | `1.0.x.*` | Legacy projects, Boot 2.x starter line |
| 2.0.x | `feature/2.0.x` | 17 | `2.0.x.*` | Main line (JDK 17) |
| 3.0.x | `feature/3.0.x` | 21 | `3.0.x.*` | New projects |

<a id="4-architecture--modules"></a>
## 4. Architecture & Modules

```text
[ Java Application ]
        |
        | opencode-java-sdk
        v
+------------------------------------------+
| OpenCodeClient (facade)                   |
|  HTTP   /session, /agent, /config,        |
|         /project, /provider, /file,       |
|         /find, /command, /skill, /mcp ... |
|  SSE    GET /event (subscribe, typed      |
|         EventHandler callbacks)           |
|  CLI    opencode run / session / serve /  |
|         auth / mcp / debug ...            |
+------------------------------------------+
        |
        v
[ OpenCode Server (opencode serve) ]
```

Single-module library (packaging `jar`). Package layout:

| Package | Responsibility |
|---|---|
| `io.github.easy4j.opencode` | Facade `OpenCodeClient` + config classes |
| `io.github.easy4j.opencode.api` | `OpenCodeHttpClient`, `OpenCodeSseClient`, `OpenCodeRequestContext` |
| `io.github.easy4j.opencode.api.model` | DTOs (`Session`, `PromptRequest`, `PromptResult`, `Agent`, `Project`, ...) |
| `io.github.easy4j.opencode.api.event` | Typed `EventHandler` |
| `io.github.easy4j.opencode.api.mapper` | Message mapping + callback parsing |
| `io.github.easy4j.opencode.cli` | CLI facade (`OpenCodeCli` / `OpenCodeCliExecutor`) |
| `io.github.easy4j.opencode.cli.availability` | CLI availability probing |
| `io.github.easy4j.opencode.exception` | Exception hierarchy |

<a id="5-installation"></a>
## 5. Installation

Maven:

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>opencode-java-sdk</artifactId>
    <version>3.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

Gradle:

```groovy
implementation 'io.github.easy4j:opencode-java-sdk:3.0.x.x.20260630-SNAPSHOT'
```

Snapshot builds require an enabled snapshot repository (Aliyun Maven snapshot repository per `distributionManagement` in `pom.xml`).

<a id="6-quick-start"></a>
## 6. Quick Start

```java
OpenCodeClientConfig config = new OpenCodeClientConfig();
config.getHttp().setServerUrl("http://localhost:4096");
config.getHttp().setPassword("your-password");   // matches OPENCODE_SERVER_PASSWORD

OpenCodeClient client = new OpenCodeClient(config);

// Health check
HealthStatus health = client.health();
System.out.println("version: " + health.getVersion());

// Create a session and chat
Session session = client.createSession("my-task");
PromptResult result = client.chatCompletion(session.getId(),
        "Explain how closures work in JavaScript");
System.out.println(result.getTextContent());

// Async prompt (no waiting)
client.chatCompletionAsync(session.getId(), "Write a hello world in Python");

// List agents
List<Agent> agents = client.listAgents();

client.close();
```

**Expected result**: with a running `opencode serve` on port 4096 (started with `OPENCODE_SERVER_PASSWORD=your-password opencode serve`), `health()` returns the server version, the chat call returns the model's text content, and the async prompt is accepted without blocking.

<a id="7-configuration"></a>
## 7. Configuration

Configuration is object-based. `OpenCodeClientConfig` aggregates `http` (an `OpenCodeHttpClientConfig`) and `cli` (an `OpenCodeCliConfig`).

`OpenCodeHttpClientConfig` properties:

| Property | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable the HTTP sub-system |
| `startupCheckEnabled` | `false` | Probe the server at startup |
| `failFastOnUnavailable` | `false` | Fail construction when the probe fails |
| `serverUrl` | `http://localhost:4096` | OpenCode Server address |
| `username` | `opencode` | HTTP Basic Auth username |
| `password` | `null` | HTTP Basic Auth password (`OPENCODE_SERVER_PASSWORD`) |
| `connectTimeoutMillis` | `15000` | Connect timeout (ms) |
| `readTimeoutMillis` | `300000` | Read timeout (ms) |
| `verifySsl` | `true` | Verify HTTPS certificates |
| `defaultModel` | `null` | Default model (`provider/model`) |
| `defaultAgent` | `null` | Default agent |

`OpenCodeCliConfig` properties:

| Property | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable the CLI sub-system |
| `startupCheckEnabled` | `false` | Probe `opencode --version` at startup |
| `failFastOnUnavailable` | `false` | Fail construction when the probe fails |
| `executable` | `opencode` | Executable name or absolute path |
| `timeout` | `300` | CLI command timeout (seconds) |
| `probeTimeoutSeconds` | `5` | Availability-probe timeout (seconds) |
| `workingDirectory` | `null` | Subprocess working directory |
| `maxConcurrentExecutions` | `0` | Max concurrent subprocesses (0 = unlimited) |

<a id="8-core-usage"></a>
## 8. Core Usage

### 8.1 SSE events with typed handler

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

### 8.2 CLI automation

```java
OpenCodeCli cli = client.cli();

// Non-interactive mode
OpenCodeCliResult result = cli.run("Explain async/await in JavaScript");
System.out.println(result.getStdout());

cli.run("Hello", "plan", "anthropic/claude-sonnet-4-5");  // agent + model
cli.sessionList();
cli.serve(4096, "127.0.0.1");                             // opencode serve --port 4096 --hostname 127.0.0.1
cli.upgrade("v1.18.0", "npm");
```

All `cli*()` methods also have facade equivalents on `OpenCodeClient` (e.g. `cliServe`, `cliModels`).

<a id="9-testing--build"></a>
## 9. Testing & Build

```bash
mvn clean verify
```

- The HTTP client is tested against MockWebServer (`src/test/java/io/github/easy4j/opencode/api/OpenCodeHttpClientTest.java`).
- JaCoCo runs `prepare-agent`, `report` and `check` on the `verify` phase with a **90% line-coverage** rule (`haltOnFailure=false`).
- Release packaging (`mvn -Prelease deploy`) attaches sources and javadoc jars, GPG-signs artifacts and is wired for Sonatype Central Publishing; plain `mvn deploy` routes SNAPSHOT/release artifacts to the Aliyun Maven repository per `distributionManagement`.

<a id="10-versioning--branches"></a>
## 10. Versioning & Branches

| Branch | Version pattern | JDK | Maintenance policy |
|---|---|---|---|
| `feature/1.0.x` (this branch) | `1.0.x.*` | 8 | Compatibility fixes and JDK-8-safe dependency upgrades only |
| `feature/2.0.x` | `2.0.x.*` | 17 | Main development line |
| `feature/3.0.x` | `3.0.x.*` | 21 | New projects |

<a id="11-contributing--license"></a>
## 11. Contributing & License

Run `mvn clean verify` before opening a pull request and describe compatibility, testing, documentation and migration impact. This project is licensed under the [Apache License 2.0](LICENSE).
