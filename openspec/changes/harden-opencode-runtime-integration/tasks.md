## 1. Establish contract baselines

- [ ] 1.1 Pin the supported OpenCode CLI/server version for this change and record the executable/version fixture used by integration tests.
- [ ] 1.2 Add upstream-contract tests for `run --share`, `--auto`, representative Web/Serve flags, and raw argument pass-through.
- [ ] 1.3 Add SSE fixtures for `message.part.delta`, `message.part.updated`, session idle, session error, and transport failure.

## 2. Correct typed CLI contracts

- [ ] 2.1 Replace the incorrect string-valued `OpenCodeRunOptions.share` model with boolean switch semantics, providing a deliberate compatibility/deprecation path if needed.
- [ ] 2.2 Add opt-in `--auto` modeling with default false.
- [ ] 2.3 Model `--interactive` without claiming PTY support and document the difference between flag coverage and terminal-session support.
- [ ] 2.4 Audit remaining typed CLI wrappers against the pinned upstream version and correct option arity mismatches.

## 3. Harden short-lived CLI execution

- [ ] 3.1 Enforce `maxConcurrentExecutions` in `OpenCodeCliExecutor`.
- [ ] 3.2 Add bounded stdout/stderr capture and truncation metadata.
- [ ] 3.3 Preserve captured output on failures when available and classify success, non-zero exit, spawn failure, timeout, and cancellation.
- [ ] 3.4 Add per-invocation working-directory and environment inheritance/override support.
- [ ] 3.5 Add unit tests for concurrency, environment isolation, large output, UTF-8 boundaries, non-zero exit, spawn failure, timeout, and cancellation.

## 4. Add streaming CLI execution

- [ ] 4.1 Introduce a streaming execution handle with incremental stdout/stderr callbacks or publishers and a separate completion future.
- [ ] 4.2 Implement incremental UTF-8-safe line/event framing suitable for `run --format json`.
- [ ] 4.3 Add cancellation and bounded buffering/backpressure behavior.
- [ ] 4.4 Add `OpenCodeCli.runStream(...)` convenience APIs and tests proving events are visible before process exit.

## 5. Add managed Web/Serve/ACP launcher lifecycle

- [ ] 5.1 Introduce a managed process handle exposing state, exit completion, bounded log tail, graceful stop, and force kill.
- [ ] 5.2 Add managed `serve` and `web` launch APIs with separate startup timeout and no inherited short-command lifetime timeout.
- [ ] 5.3 Implement HTTP readiness probing and clean startup-failure teardown.
- [ ] 5.4 Define random-port behavior: reliable discovery if supported by a stable upstream contract; otherwise require explicit port in the managed API.
- [ ] 5.5 Add ACP managed-launch support while explicitly deferring a full ACP protocol client.
- [ ] 5.6 Add lifecycle tests for readiness success, readiness failure, graceful stop, forced stop, unexpected exit, and log-tail bounds.

## 6. Correct HTTP/SSE chat streaming

- [ ] 6.1 Treat canonical text delta events as append operations and stop appending full `message.part.updated` snapshots as deltas.
- [ ] 6.2 Extend SSE subscriptions with explicit connected/readiness completion.
- [ ] 6.3 Propagate SSE transport failure to the active streaming chat result.
- [ ] 6.4 Ensure prompt submission occurs only after SSE readiness.
- [ ] 6.5 Keep local cancellation separate from explicit server-side `abortSession`.
- [ ] 6.6 Add race tests for immediate cancellation, connection failure before readiness, transport drop during generation, and duplicate snapshot/delta sequences.

## 7. Strengthen configuration modeling

- [ ] 7.1 Add a server configuration model matching the pinned upstream schema while retaining an extension map.
- [ ] 7.2 Implement real unknown-property capture and serialization using the correct Jackson mechanism for the 3.0.x line.
- [ ] 7.3 Add read-modify-write round-trip tests proving unknown fields are preserved.

## 8. Documentation and branch compatibility

- [ ] 8.1 Update README/API examples to distinguish short command execution, streaming execution, managed server lifecycle, and interactive terminal limitations.
- [ ] 8.2 Verify `feature/3.0.x` with full unit tests and enabled OpenCode integration-contract tests.
- [ ] 8.3 Port behavioral changes to `feature/2.0.x` using Java 17/Jackson 2 APIs and rerun equivalent tests.
- [ ] 8.4 Resolve the `feature/1.0.x` JDK baseline conflict before porting; document the chosen baseline and make CI/POM/README agree.
- [ ] 8.5 Port applicable behavior to `feature/1.0.x` and run the branch-specific build/test matrix.
- [ ] 8.6 Run OpenSpec validation and archive/sync the capability specs only after implementation and verification pass.
