## Context

The SDK currently has three independent integration channels:

1. HTTP REST through `OpenCodeHttpClient`.
2. SSE through `OpenCodeSseClient`.
3. Local CLI through `OpenCodeCli` and `OpenCodeCliExecutor`.

The public surface is broad, but all CLI commands ultimately use one synchronous capture executor. This is suitable for short commands but not for event streaming, interactive sessions, or long-running servers. The HTTP chat facade also treats more than one event shape as append-only text, while upstream exposes explicit delta events.

## Goals

- Make the modeled Java API match upstream OpenCode command and event contracts.
- Provide safe process execution for short-lived automation.
- Provide streaming process execution without waiting for exit.
- Provide lifecycle-managed long-running OpenCode processes.
- Preserve independent HTTP, SSE, and CLI channels.
- Keep old branch compatibility constraints explicit rather than hiding them behind one implementation.

## Non-Goals

- Reimplement the OpenCode Web UI in Java.
- Make ACP a full protocol client in this change; this change provides a correct managed launcher boundary. A full ACP protocol client can be a separate capability.
- Automatically abort a server-side session whenever a local SSE subscriber disconnects.
- Default-enable `--auto` or any permission-bypassing option.
- Promise multi-tenant security solely from per-process config or environment isolation.

## Decisions

### 1. Separate three CLI execution modes

```text
OpenCodeCli
   |
   +-- execute(...)       short command, bounded capture
   |
   +-- stream(...)        incremental stdout/stderr + exit future
   |
   +-- startManaged(...)  long-running process handle
```

The existing synchronous `execute` path remains the default for short commands.

### 2. Concurrency belongs to the executor instance

`maxConcurrentExecutions > 0` is enforced by a semaphore owned by `OpenCodeCliExecutor`. A permit is acquired before spawn and released after process termination or spawn failure.

This prevents configuration that appears to limit concurrency while having no runtime effect.

### 3. Output capture is bounded independently from debug-log truncation

Debug log truncation only protects log messages; it does not bound process capture memory.

Add explicit capture limits:

- `maxStdoutBytes`
- `maxStderrBytes`

When the limit is exceeded, the result MUST mark the stream as truncated. Streaming consumers still receive data according to their configured mode, while retained tail/head policy is deterministic.

### 4. Environment configuration is per executor invocation

Add an execution context with:

- inherit parent environment: boolean
- environment overrides: map
- working directory override
- optional stdin mode

Sensitive values MUST NOT be copied into debug output.

### 5. Long-running processes have no ordinary command timeout

A managed process uses separate time concepts:

- startup timeout: time to become ready
- graceful shutdown timeout: time allowed after stop request
- lifetime: unbounded unless the caller explicitly sets one

`OpenCodeServerHandle` exposes process state, actual address when known, exit future, stop/kill operations, and bounded log tail.

### 6. Server readiness is protocol-based when possible

For `serve` and `web`, readiness SHOULD be established via HTTP health probing against the resolved address instead of assuming that process spawn means service readiness.

For random ports, the launcher MUST obtain the actual bound address from a stable OpenCode output contract or a supplied fixed port. If no reliable random-port discovery is available, the managed API MUST require an explicit port rather than parse arbitrary human-oriented text heuristically.

### 7. CLI option arity is modeled exactly

`--share` is a boolean switch.

`--auto` is a boolean switch and defaults to false.

Interactive mode is modeled separately from the ordinary non-interactive result API; adding `--interactive` to an options object alone does not imply full PTY support.

### 8. SSE chat consumes deltas, snapshots update state

Text accumulation is driven by delta events such as `message.part.delta` and the version-specific canonical text-delta event.

`message.part.updated` is treated as a snapshot/state update and MUST NOT be blindly appended to accumulated text.

### 9. SSE subscription readiness is explicit

A subscription exposes:

- connected/readiness future
- terminal failure future or callback
- cancellation

The chat flow becomes:

```text
ensure session
  -> create subscription
  -> await SSE connected
  -> submit prompt_async
  -> consume deltas
  -> idle => complete
  -> session error / transport error => fail
```

### 10. Client-side unsubscribe and server-side abort remain separate

Cancelling a local stream MUST close its local subscription and timers.

Aborting the OpenCode session is an explicit operation because another client may still be observing or relying on that server-side execution.

A future convenience API may opt into “cancel and abort”, but it must be named explicitly.

### 11. Configuration model preserves unknown fields

The typed configuration model gains a `server` field.

Unknown properties MUST be captured using Jackson's any-setter / any-getter mechanism appropriate to each branch's Jackson major version. This allows read-modify-write flows without silently discarding newly introduced upstream configuration fields.

## Compatibility Strategy

### feature/3.0.x

- Java 21
- Jackson 3 `tools.jackson.*`
- primary implementation and integration tests

### feature/2.0.x

- Java 17
- Jackson 2
- same public behavior, compatibility-specific imports/APIs

### feature/1.0.x

The branch metadata currently conflicts: CI describes a JDK 8 line while its POM compiler release is 17. Before backporting, the branch baseline MUST be corrected or explicitly redefined. Runtime hardening MUST NOT be declared complete on this branch until its build baseline is resolved and verified.

## Failure Model

Short CLI execution MUST distinguish at least:

- success
- non-zero exit
- spawn failure
- timeout
- cancellation

Streaming/managed execution MUST surface terminal process failure separately from individual stderr lines.

SSE streaming MUST distinguish:

- OpenCode session error event
- transport failure
- JSON/event decoding failure according to configured strictness
- local cancellation
- completion by idle event

## Security

- `--auto` MUST default to false.
- passwords and environment secrets MUST never appear in SDK debug output.
- exposing Web/Serve on non-loopback interfaces SHOULD require explicit caller configuration.
- CORS configuration and authentication are independent concerns and MUST NOT be conflated.

## Open Questions

No unresolved question blocks P0 implementation.

A full ACP Java protocol client is intentionally deferred to a separate proposal.
