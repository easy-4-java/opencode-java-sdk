# OpenCode 运行时集成加固提案

## Why

已有 SDK 命令入口较多，但原始基线存在参数契约、流式事件处理、子进程管理和配置往返的缺口。此次先将这些行为写成可评审、可测试的 OpenSpec，再决定实现，避免以“已有方法或提交”替代完成证明。

## What Changes

- 精确建模 CLI 参数：布尔 `--share`、显式启用的 `--auto`、交互参数和版本受控的网络/全局选项；保留 raw argv。
- 补有界输出、并发准入、每次调用独立环境、退出分类、取消与实时 CLI 输出。
- 为 Web/Serve 建立启动、就绪、退出和关闭契约，分离启动超时与进程存活时间。
- 为 ACP 提供双向 stdio 启动边界，不声称已经实现完整 ACP Java 协议客户端。
- 修正 SSE 文本增量/快照、连接就绪、终态错误、取消、会话关联与资源释放。
- 补 server 配置及未知 JSON 字段保留，同时保留通用配置提交入口。
- 建立固定版本的上游契约、真实子进程及三兼容分支验证。
- **BREAKING（行为纠错）**：旧 `share(String)` 只兼容 null/true/false；其他值明确拒绝，不再把任意字符串当作分享范围。新接口使用 boolean。
- 有界缓冲和明确错误分类可能改变旧调用者观察到的截断、空白及失败结果，必须提供迁移说明，不删除原有方法签名。

## Capabilities

### New Capabilities

此处 New 表示首次进入 OpenSpec 主规范体系，不代表对应业务以前完全不存在。读取的草稿只有 change，没有 `openspec/specs` 主规范，因此六份 delta 都用 ADDED，不伪造 MODIFIED 基线。

- `cli-contract`：类型化参数、原始参数、危险开关与终端模式边界。
- `cli-runtime`：并发、输出界限、调用环境、流式执行与错误分类。
- `server-lifecycle`：Web/Serve 管理、认证、资源所有权及 ACP 启动边界。
- `chat-streaming`：SSE 就绪、事件归约、失败、取消与同会话隔离。
- `configuration-model`：强类型 server、未知字段和 PATCH 边界。
- `contract-verification`：证据、评审门禁、固定上游版本及三线兼容验证。

### Modified Capabilities

无。后续已有主规范且需求改变时，才使用 MODIFIED 并完整保留既有场景。

## Scope and Non-Goals

本提案覆盖上面六个能力。完整 ACP 协议客户端、内嵌 PTY/终端尺寸控制、Web UI 重做、跨主机服务注册、多租户安全平台、无服务端支持的 SSE 精确一次重放均不在本次范围；这些内容需要独立 proposal。

## Impact

源码锚点：`feature/1.0.x@05702c10fc2533c9dfb4f58be2b97f1247a4c9b3`、`feature/2.0.x@13f68b1b506dfb0aa274d388f7f5a028d340b67b`、`feature/3.0.x@99974efc311ba8c0aabad8c9471ed803d155e534`。
本轮纯文档分支基于第三个提交；从已有实现分支复用草稿，但不带入其代码提交。
建议以 OpenCode `v1.17.18` 为契约目标；真实二进制、摘要和测试证据尚未记录，不声称通过兼容验证。
1.0.x 的 JDK 基线冲突是该分支实现与发布的阻塞条件，不能擅自用提高版本号代替解决。

## Approval Status

DRAFT / NOT_APPROVED。现有实现仅为待核验候选，不作为规范已经满足的证据。
本轮只交付规范和计划；下一次实施前完成 `review.md` 门禁。事实及出处见 `evidence.md`，技术方案见 `design.md`。
