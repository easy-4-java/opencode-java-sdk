# Delta for CLI Contract

## MODIFIED Requirements

### Requirement: Typed CLI options match upstream option arity

The SDK MUST model each typed OpenCode CLI option using the same value arity and semantic type as the supported upstream OpenCode version.

#### Scenario: Share session

- GIVEN a caller enables session sharing
- WHEN `OpenCodeRunOptions` is converted to CLI arguments
- THEN the arguments MUST contain `--share`
- AND MUST NOT append an arbitrary value after `--share`

#### Scenario: Auto approval remains opt-in

- GIVEN a caller does not explicitly enable automatic approval
- WHEN run arguments are generated
- THEN `--auto` MUST NOT be emitted

#### Scenario: Auto approval enabled

- GIVEN a caller explicitly enables automatic approval
- WHEN run arguments are generated
- THEN `--auto` MUST be emitted exactly once

### Requirement: Typed wrappers remain extensible

The SDK MUST retain a raw argument escape hatch for upstream commands or flags that have not yet received a typed wrapper.

#### Scenario: Unknown future option

- GIVEN a future OpenCode flag is not modeled by the current SDK
- WHEN the caller uses the raw CLI entry point
- THEN the SDK MUST pass the provided argument vector without shell interpolation
