# Delta for Chat Streaming

## MODIFIED Requirements

### Requirement: Chat text is accumulated from canonical delta events

The chat streaming client MUST append text only from events that represent text deltas for the supported OpenCode version.

#### Scenario: Part delta received

- GIVEN an event with type `message.part.delta`
- AND its field represents text
- WHEN the event belongs to the active session
- THEN the delta value MUST be appended exactly once

#### Scenario: Part snapshot received

- GIVEN an event with type `message.part.updated`
- WHEN it contains a full part snapshot
- THEN the snapshot MUST NOT be blindly appended as a new text delta

### Requirement: Prompt submission waits for event-stream readiness

The chat streaming client MUST wait for SSE connection readiness before sending `prompt_async`.

#### Scenario: SSE cannot connect

- GIVEN a session has been selected
- AND the SSE connection fails before becoming ready
- WHEN a streaming chat is started
- THEN `prompt_async` MUST NOT be submitted
- AND the streaming result MUST fail with the transport error

### Requirement: SSE terminal failures propagate to the chat result

Transport failure after connection MUST complete the corresponding streaming chat exceptionally unless the chat has already reached a terminal state.

#### Scenario: Connection drops during generation

- GIVEN streaming generation is active
- WHEN the SSE transport fails
- THEN the streaming result MUST fail without waiting only for the overall read timeout

### Requirement: Local cancellation does not implicitly abort shared server work

Cancelling the local stream MUST release local SSE and timeout resources, while server-side session abort remains explicit.

#### Scenario: Local subscriber cancels

- GIVEN a server-side session is generating
- WHEN one local subscriber cancels its stream
- THEN the local subscription MUST close
- AND the SDK MUST NOT implicitly invoke session abort
