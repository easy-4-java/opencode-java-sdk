# Delta for Server Lifecycle

## ADDED Requirements

### Requirement: Long-running OpenCode processes use managed lifecycle semantics

The SDK MUST provide a managed process abstraction for long-running OpenCode launch modes instead of applying the ordinary short-command timeout.

#### Scenario: Web process remains alive

- GIVEN a caller starts OpenCode Web through the managed API
- WHEN startup succeeds
- THEN the process MUST remain alive beyond the short-command timeout
- UNTIL the caller stops it, the process exits, or an explicit lifetime policy terminates it

### Requirement: Readiness is distinct from process spawn

The SDK MUST expose readiness independently from successful process creation.

#### Scenario: Process starts but health endpoint is unavailable

- GIVEN the child process was spawned
- AND the expected OpenCode HTTP endpoint does not become healthy before startup timeout
- WHEN the caller awaits readiness
- THEN readiness MUST fail
- AND the process MUST be cleaned up according to the startup-failure policy

### Requirement: Managed processes expose deterministic shutdown

The SDK MUST provide graceful stop and force-kill operations with observable exit completion.

#### Scenario: Graceful stop times out

- GIVEN a managed process does not exit after a graceful stop request
- WHEN the graceful shutdown timeout elapses
- THEN the caller MUST be able to force terminate the process
- AND the final exit state MUST be observable
