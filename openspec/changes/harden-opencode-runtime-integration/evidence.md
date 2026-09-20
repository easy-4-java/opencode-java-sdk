# 源码基线、出处与修订说明

核对日期：2026-09-20。以下是本次读取时的快照，分支后续可能移动。

## 仓库锚点

仓库：https://github.com/easy-4-java/opencode-java-sdk

| 分支 | 固定提交 | 本轮用途 |
|---|---|---|
| feature/1.0.x | 05702c10fc2533c9dfb4f58be2b97f1247a4c9b3 | 兼容契约参照；JDK 基线冲突未解决 |
| feature/2.0.x | 13f68b1b506dfb0aa274d388f7f5a028d340b67b | Java 17/Jackson 2 兼容线参照 |
| feature/3.0.x | 99974efc311ba8c0aabad8c9471ed803d155e534 | 本次纯文档分支基线 |
| fix/runtime-integration-hardening | 6c590c3a9af2a595d729cb6e50b1fd6d3458459e | 已有草稿与提前实现的来源，不纳入本次代码变更 |

比较最后两项：实现分支领先 34 个提交，净变更 29 个文件，其中 OpenSpec 文档 11 个、主代码 11 个、测试 7 个。这是文件/提交差异统计，不是已完成能力或测试通过数量。
原始源码树：b778e00248132beda29e73138df454204b5e6209。
原草稿 openspec 树：cd382f6f06f399e03a5b191a2f3f28913a68e774。

## 已有实现的核验范围

主代码：OpenCodeCliConfig、OpenCodeChatClient、OpenCodeSseClient、OpenCodeConfig、OpenCodeServerConfig、SseSubscription、OpenCodeCliExecutionContext、OpenCodeCliExecutor、OpenCodeCliResult、OpenCodeCliStreamHandle、OpenCodeRunOptions。
测试：OpenCodeChatClientStreamingContractTest、OpenCodeSseReadinessContractTest、OpenCodeConfigContractTest、OpenCodeCliExecutorRuntimeContractTest、OpenCodeCliStreamingContractTest、OpenCodeCliTest、OpenCodeRunOptionsContractTest。
这些文件只证明改动存在。本轮没有运行它们，也没有把它们带入文档分支。

## 原草稿问题及本稿处理

1. 只有 changes，没有对应主 specs；cli-contract、chat-streaming、configuration-model 却使用 MODIFIED。本稿保留同一个 change id，六项首次纳管契约统一声明 New/ADDED。
2. contract-verification 的跨分支 Requirement 没有 Scenario。本稿为每条 Requirement 补齐可观察 WHEN/THEN 场景。
3. config.yaml 原有 project/conventions 自定义键不能代替官方文档约定的 context/rules 注入。本稿改用 schema/context/rules；不声称已运行官方验证证明旧 YAML 被拒绝。
4. 原任务缺少明确书面批准门禁，部分项没有自己的验证方法。本稿加入 review.md、逐项需求编号和验收证据，所有任务保持未勾选。
5. 原稿部分决策仍是二选一。本稿明确首版 managed 要求固定端口、share 迁移规则、默认资源策略；这些设计仍待用户批准。

## 外部参考

- 用户参考 CLI：https://open-code.ai/en/docs/cli
- 用户参考 Web：https://open-code.ai/en/docs/web
- 上游 CLI：https://opencode.ai/docs/cli/
- 上游 Web：https://opencode.ai/docs/web/
- 目标版本 run 解析器：https://github.com/anomalyco/opencode/blob/v1.17.18/packages/opencode/src/cli/cmd/run.ts
- 目标版本事件类型：https://github.com/anomalyco/opencode/blob/v1.17.18/packages/sdk/js/src/v2/gen/types.gen.ts
- OpenSpec 工作流 schema：https://github.com/Fission-AI/OpenSpec/blob/main/schemas/spec-driven/schema.yaml
- OpenSpec 配置说明：https://github.com/Fission-AI/OpenSpec/blob/main/docs/opsx.md
- OpenSpec 规范约定：https://github.com/Fission-AI/OpenSpec/blob/main/openspec/specs/openspec-conventions/spec.md

网页与 main 文档会变化；实施时记录读取版本和二进制摘要。当前 v1.17.18 是候选固定契约基线，不是已运行认证结果。

## 证据边界

已做：GitHub 分支/树/文件/差异读取；形成纯文档评审稿。
未做：本机 CodeGraph、Java 构建、真实 OpenCode、OpenSpec 官方 CLI 严格校验、漏洞/泄漏动态验证。
环境探测：node/npm/git 可用，openspec 未安装；npm registry DNS 解析失败。不得用自写检查替代官方校验或行为测试。
