# 聊天流式契约

## Purpose

定义 SSE 就绪、文本归约、请求关联和终态，避免丢片、重复和资源遗留。

## ADDED Requirements

### Requirement: CHAT-01 Canonical text reduction

SDK MUST 按支持版本识别文本增量，并按会话、消息、Part 和字段归约；快照不能被无条件重复追加。

#### Scenario: Text delta

- **WHEN** 收到属于当前生成的 message.part.delta 正文字段
- **THEN** 正文按顺序更新，非正文增量不混入回答。

#### Scenario: Snapshot plus delta

- **WHEN** 收到同一 Part 的完整快照和后续文本增量
- **THEN** 状态正确更新，累计正文不因快照重复拼接；重复快照也不新增文本。

#### Scenario: Unrelated event

- **WHEN** 收到其他会话、其他生成或未知类型事件
- **THEN** 不混入当前回答，未知事件保留通用观察能力。

### Requirement: CHAT-02 Ready before submission

SDK MUST 在事件订阅已就绪后才提交异步生成，且建立会话和订阅阶段都能失败或超时。

#### Scenario: Delayed connection

- **WHEN** 订阅连接尚未就绪
- **THEN** 不提交 prompt_async，连接就绪后只提交一次。

#### Scenario: Failure before readiness

- **WHEN** 连接失败、启动超时或就绪前取消
- **THEN** 不提交生成，返回对应失败或取消并清理已创建资源。

### Requirement: CHAT-03 Terminal propagation

SDK MUST 将正常完成、服务端错误、传输错误、异常 EOF 和本地取消分别转为可观察终态。

#### Scenario: Transport drop

- **WHEN** 生成中连接断开且没有正常完成事件
- **THEN** 及时失败，不只等待总超时，也不误报成功。

#### Scenario: Structured idle

- **WHEN** 收到属于当前生成的对象型 status.type=idle 或该版本支持的 idle 事件
- **THEN** 按上游契约结束，不依赖对象字符串化。

#### Scenario: Callback exception

- **WHEN** 调用方消费者抛错
- **THEN** 报告消费错误而非吞掉或误标为解码失败。

### Requirement: CHAT-04 Explicit cancellation

SDK MUST 区分本地停止订阅与服务端中止，并在所有竞争终态中至多完成一次、释放一次。

#### Scenario: Cancel local stream

- **WHEN** 调用者取消本地流
- **THEN** 停止后续回调并回收订阅、计时器和监听引用，不自动 abort 共享服务端会话。

#### Scenario: Finish cancel race

- **WHEN** 完成、取消和网络错误并发到达
- **THEN** 结果只保留一个终态，不残留订阅或未完成 Future。

### Requirement: CHAT-05 Session and replay boundary

SDK SHALL 拒绝同一客户端内同一会话的重叠高层生成，并明确外部并发和断线重放限制。

#### Scenario: Overlapping local generation

- **WHEN** 当前会话尚有高层生成时再次提交
- **THEN** 返回明确忙错误而非混用事件。

#### Scenario: No replay guarantee

- **WHEN** 断线后没有服务端重放或去重依据
- **THEN** 不自动重发 prompt，也不宣称精确一次恢复。
