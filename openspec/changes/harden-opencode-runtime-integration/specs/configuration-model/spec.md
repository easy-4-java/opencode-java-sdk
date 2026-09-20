# 配置模型契约

## Purpose

在强类型访问与上游扩展兼容之间保持可靠配置往返。

## ADDED Requirements

### Requirement: CFG-01 Typed server settings

SDK SHALL 提供与目标版本一致的 server 配置访问，并保留通用配置提交能力。

#### Scenario: Read server settings

- **WHEN** 读取包含端口、监听地址及 CORS 的配置
- **THEN** 调用者可通过明确类型访问，动态提交入口仍然可用。

### Requirement: CFG-02 Unknown field preservation

SDK MUST 保留根配置和 server 内的未知属性及其 JSON 结构，不自动嵌套到 extra。

#### Scenario: Round trip

- **WHEN** 配置包含未知对象、数组、布尔或 null，修改已知字段后再序列化
- **THEN** 未显式修改的未知属性仍位于原键和原层级。

#### Scenario: Known key collision

- **WHEN** 扩展字段与已知字段同名
- **THEN** 应用明确冲突规则，不输出重复键或静默覆盖已知值。

### Requirement: CFG-03 Patch semantics

SDK SHALL 按目标上游契约区分省略、显式 null 和删除意图，不把完整读取结果自动视为安全更新请求。

#### Scenario: Partial change

- **WHEN** 只修改一个配置项
- **THEN** 不会隐式清空其他配置，序列化往返测试与实际 PATCH 行为分别验证。

#### Scenario: Provider or agent object

- **WHEN** 目标版本返回对象型 provider/agent 配置
- **THEN** 契约 fixture 必须验证该 JSON 形状，不能只验证 server 字段就宣称全配置兼容。
