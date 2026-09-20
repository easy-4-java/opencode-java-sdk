# CLI 参数契约

## Purpose

以固定 OpenCode 版本定义可测试的参数行为，而非仅证明字符串拼装。

## ADDED Requirements

### Requirement: CLI-01 Boolean sharing

SDK SHALL 将分享作为布尔开关，并对旧字符串入口提供明确迁移。

#### Scenario: Enable sharing

- **WHEN** 调用者启用分享
- **THEN** argv 包含一次 --share，后面不附加分享范围值。

#### Scenario: Legacy input

- **WHEN** 旧入口收到 null、true、false 或其他字符串
- **THEN** 前三类按约定布尔语义转换，其他值在启动前明确拒绝，不修改提示正文。

### Requirement: CLI-02 Explicit permission approval

SDK MUST 默认不启用自动批准权限，且只在调用者显式设置时传递对应开关。

#### Scenario: Default permissions

- **WHEN** 调用者未设置自动批准
- **THEN** argv 不包含 --auto 或其他跳过权限的别名。

#### Scenario: Explicit approval

- **WHEN** 调用者显式选择已支持版本的 --auto
- **THEN** 该开关仅出现一次，并保留其危险行为说明。

### Requirement: CLI-03 Version scoped options

SDK SHALL 为已核验的目标版本提供一致的 run、TUI、serve、web、ACP 及全局选项；不把滚动文档中的所有选项无条件认作已支持。

#### Scenario: Network and global flags

- **WHEN** 使用经核验的网络、CORS 或全局日志选项
- **THEN** 值类型、重复参数和作用位置与目标版本一致，不影响 SDK 自身日志配置。

#### Scenario: Optional model count

- **WHEN** 统计选项分别选择不显示、全部或前 N 个模型
- **THEN** 生成三种可区分且被目标版本接受的 argv。

#### Scenario: Unsupported version

- **WHEN** 某选项没有目标版本的支持证据
- **THEN** 不能列入已验证支持清单，类型化调用应明确拒绝或报告未支持。

### Requirement: CLI-04 Raw argument fidelity

SDK MUST 保持原始参数边界且不通过 shell 插值执行用户提供的参数。

#### Scenario: Spaces and symbols

- **WHEN** 可执行路径或参数包含空格、中文或 shell 特殊字符
- **THEN** 路径作为单一可执行文件，参数逐项原样传给子进程，不执行额外 shell 命令。

### Requirement: CLI-05 Interactive execution boundary

SDK SHALL 将交互式执行与普通捕获式调用分开，并准确报告终端支持范围。

#### Scenario: No terminal input

- **WHEN** 捕获模式收到需要交互输入的选项且没有可用输入通道
- **THEN** 在启动前拒绝或要求显式执行模式，不无限等待。

#### Scenario: Inherited terminal

- **WHEN** 调用者选择继承可用终端
- **THEN** 输入输出连接到该终端，但接口文档不声称提供内嵌 PTY 或 ACP 协议客户端。
