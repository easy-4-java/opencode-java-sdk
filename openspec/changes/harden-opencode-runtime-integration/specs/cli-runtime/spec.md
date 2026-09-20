# Delta for CLI Runtime

## ADDED Requirements

### Requirement: CLI concurrency limits are enforced

The executor MUST enforce `maxConcurrentExecutions` when the configured value is greater than zero.

#### Scenario: Limit reached

- GIVEN the concurrency limit is N
- AND N executions are active
- WHEN another execution is submitted
- THEN it MUST wait for capacity or fail according to the selected admission policy
- AND MUST NOT spawn an additional child process before capacity is available

### Requirement: CLI output retention is bounded

The executor MUST bound retained stdout and stderr independently.

#### Scenario: Child output exceeds configured capture limit

- GIVEN a configured output capture limit
- WHEN a child emits more bytes than the limit
- THEN retained output MUST remain within the configured bound
- AND the result MUST indicate truncation

### Requirement: CLI execution supports per-invocation environment isolation

The executor MUST allow a caller to control environment inheritance and provide environment overrides without mutating the JVM process environment.

#### Scenario: Override one variable

- GIVEN parent environment inheritance is enabled
- AND an execution override defines a variable
- WHEN the child process starts
- THEN the child MUST see the override value
- AND subsequent unrelated executions MUST NOT inherit that override unless configured

### Requirement: Streaming execution delivers data before process exit

The SDK MUST provide an execution mode that exposes stdout and stderr incrementally while the process is running.

#### Scenario: Long running JSON output

- GIVEN an OpenCode process emits multiple JSON event lines over time
- WHEN the caller uses streaming execution
- THEN each complete event line MUST be observable before process termination
- AND process completion MUST be available through a separate completion result

### Requirement: Process outcomes are classified

The SDK MUST distinguish success, non-zero exit, spawn failure, timeout, and cancellation.

#### Scenario: Spawn failure

- GIVEN the configured executable does not exist
- WHEN execution is attempted
- THEN the result or completion failure MUST identify spawn failure
- AND MUST NOT report it as a normal non-zero OpenCode exit
