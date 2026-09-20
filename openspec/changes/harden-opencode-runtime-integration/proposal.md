## Why

The SDK already exposes broad OpenCode HTTP, SSE and CLI coverage, but several integration contracts are incomplete or inconsistent with OpenCode v1.17.18. The most important gaps are no longer missing command wrappers: they are incorrect CLI option modeling, fragile streaming semantics, lack of bounded and isolated CLI execution, and treating long-running Web/Serve/ACP processes as ordinary blocking commands.

This change hardens the SDK so Java applications can embed OpenCode reliably rather than only invoke it as a thin command wrapper.

## What Changes

- Correct the `opencode run --share` model from a string-valued option to a boolean switch.
- Add typed support for currently unmodeled documented execution flags needed by programmatic integrations, including `run --auto` and `run --interactive`.
- Make CLI execution honor `maxConcurrentExecutions`.
- Add per-execution environment overrides and explicit environment inheritance semantics.
- Add bounded stdout/stderr capture so an untrusted or long-running child process cannot grow memory without limit.
- Preserve captured stdout/stderr and distinguish timeout, cancellation, spawn failure, and non-zero exit outcomes.
- Add a streaming CLI execution API that delivers stdout/stderr incrementally before process exit.
- Add a managed long-running process abstraction for `serve`, `web`, and ACP launch use cases with start, readiness, stop, exit, and log-tail semantics.
- Correct HTTP/SSE chat streaming to consume canonical delta events without appending full part snapshots as text deltas.
- Add SSE readiness and failure propagation so a prompt is not submitted until the event stream is connected and transport failure completes the chat stream exceptionally.
- Keep “unsubscribe from events” separate from “abort server-side session execution”; abort remains explicit.
- Add a typed `server` configuration model and real unknown-field preservation for OpenCode configuration round-trips.
- Add real OpenCode contract tests in addition to existing `echo`-based argument-construction tests.
- Apply behavioral changes first to `feature/3.0.x`, then port equivalent behavior to `feature/2.0.x` and `feature/1.0.x` while preserving each line's Java/Jackson compatibility.

## Capabilities

### New Capabilities

- `cli-runtime`: bounded, cancellable, concurrent, environment-isolated CLI process execution with streaming and managed-process modes.
- `server-lifecycle`: managed lifecycle for long-running OpenCode `serve` / `web` / ACP launcher processes.
- `contract-verification`: version-pinned OpenCode CLI and SSE contract tests beyond command-string construction.

### Modified Capabilities

- `cli-contract`: typed OpenCode CLI flags MUST match upstream option arity and semantics.
- `chat-streaming`: SSE chat streaming MUST use canonical delta events, wait for subscription readiness, and propagate transport failures.
- `configuration-model`: OpenCode configuration MUST model server settings and preserve unknown fields across read/modify/write operations.

## Impact

- Public API: `OpenCodeRunOptions.share(String)` is an incorrect API and requires a compatibility migration to `share(boolean)`. The string overload may be temporarily deprecated if source compatibility is required.
- Runtime behavior: CLI commands become concurrency-controlled and output-bounded; long-running commands gain a separate lifecycle API instead of relying on the ordinary synchronous timeout path.
- Tests: contract tests require a pinned OpenCode test binary or explicitly enabled integration-test profile.
- Branching: implementation starts on the Java 21 / Jackson 3 line, then is backported with compatibility-specific code for the older branches.
