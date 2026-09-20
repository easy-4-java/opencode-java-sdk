# 服务生命周期契约

## Purpose

将常驻 OpenCode 服务与普通命令区分，明确启动、认证、所有权和关闭。

## ADDED Requirements

### Requirement: SRV-01 Separate timeout domains

SDK SHALL 分别配置服务启动超时、关闭宽限和可选存活上限。

#### Scenario: Managed lifetime

- **WHEN** managed Web/Serve 已就绪且超过普通命令超时
- **THEN** 没有显式存活上限时仍保持运行。

### Requirement: SRV-02 Owned endpoint readiness

SDK MUST 仅在所启动实例的目标端点健康且子进程仍存活时报告服务就绪。

#### Scenario: Occupied port

- **WHEN** 预期端口已经有其他服务占用
- **THEN** 本次启动明确失败，不把其他服务的健康状态当作本次成功。

#### Scenario: Readiness timeout

- **WHEN** 新进程启动但端点未在期限内健康
- **THEN** 就绪失败且回收本次拥有的进程。

#### Scenario: Random port without discovery

- **WHEN** 首版 managed 调用没有明确非零端口且无已验证地址发现协议
- **THEN** 启动前返回明确参数错误，不用猜测的地址宣告就绪。

### Requirement: SRV-03 Ownership aware shutdown

SDK MUST 暴露可观察的退出终态及幂等关闭，并区分自建进程与外部服务。

#### Scenario: Owned shutdown

- **WHEN** 自建进程在关闭宽限后仍不退出
- **THEN** 按调用者明确选择的强制终止策略处理并报告清理结果。

#### Scenario: External connection closes

- **WHEN** 关闭连接到外部 OpenCode 的客户端句柄
- **THEN** 只释放客户端资源，不终止外部服务。

#### Scenario: Unexpected exit

- **WHEN** 服务在 READY 状态自行退出
- **THEN** 退出 Future 和状态反映真实终态，不继续报告可用。

### Requirement: SRV-04 Safe network exposure

SDK MUST 默认限制本机访问，将网络开放、服务端认证和 CORS 作为独立选项。

#### Scenario: LAN or mDNS

- **WHEN** 调用者请求非 loopback 或 mDNS
- **THEN** 要求显式开放及认证配置；无认证运行需另行显式选择不安全策略。

#### Scenario: Diagnostic redaction

- **WHEN** 生成启动诊断日志
- **THEN** 不输出密码、令牌、完整环境或内联配置，客户端认证不被误当成服务端认证已启用。

### Requirement: SRV-05 ACP launcher boundary

SDK SHALL 为 ACP launcher 保留双向 stdio、独立 stderr 和退出/取消控制，不将进程启动等同协议握手完成。

#### Scenario: Protocol output

- **WHEN** 外部 ACP 客户端通过 launcher 通信
- **THEN** stdout 不被当日志消费，协议字节交给客户端，stderr 单独传递。

#### Scenario: Spawned ACP

- **WHEN** ACP 子进程启动成功但未完成协议初始化
- **THEN** 仅报告进程已启动，不报告 ACP 会话已经就绪。
