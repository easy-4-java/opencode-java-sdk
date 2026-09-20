# Delta for Configuration Model

## MODIFIED Requirements

### Requirement: Server configuration is represented explicitly

The configuration DTO MUST expose OpenCode server configuration using a typed or typed-plus-extensible model appropriate to the upstream schema.

#### Scenario: Read server config

- GIVEN the server returns a configuration object containing `server`
- WHEN the SDK deserializes it
- THEN callers MUST be able to access that server configuration without manually traversing a raw root map

### Requirement: Unknown configuration fields survive round trip

The SDK MUST preserve unknown configuration properties during read-modify-write workflows.

#### Scenario: Upstream introduces a new root property

- GIVEN the OpenCode server returns a root configuration property unknown to this SDK version
- WHEN the SDK deserializes and later serializes the configuration object
- THEN the unknown property MUST remain present unless the caller explicitly removes it
