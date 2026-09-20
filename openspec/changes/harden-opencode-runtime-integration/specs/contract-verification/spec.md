# Delta for Contract Verification

## ADDED Requirements

### Requirement: CLI contract tests execute against a pinned OpenCode version

The project MUST include opt-in integration tests that execute against the OpenCode version the SDK claims to support.

#### Scenario: Validate typed flag arity

- GIVEN the pinned OpenCode test binary
- WHEN typed option combinations are exercised
- THEN the test MUST verify that OpenCode accepts the generated argument vector
- AND MUST NOT rely only on `echo` output

### Requirement: SSE fixtures cover supported event forms

The project MUST maintain event fixtures or integration tests for canonical text delta, snapshot, idle, session error, and transport failure behavior.

#### Scenario: Delta regression test

- GIVEN a canonical upstream delta event fixture
- WHEN it is processed by the Java chat streaming layer
- THEN the resulting accumulated text MUST match the expected value exactly once

### Requirement: Compatibility lines share behavioral tests

Behavioral requirements that are common across SDK version branches MUST be represented by equivalent tests on each maintained line, with only Java/Jackson compatibility differences in implementation.
