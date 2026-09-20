# 评审门禁与交付状态

## 当前状态

- 工件：书面评审稿；没有新增规范批准记录。
- 人工批准：NOT_APPROVED。提交推送授权不等于对每条行为规范和实现结果的验收。
- 实现：PAUSED；已有代码为候选，本次不新增或重写业务源码及测试。
- 交付目标：将既有候选代码与修订后的 OpenSpec 文档汇总到 `fix/runtime-integration-hardening`。
- OpenSpec 官方 CLI 严格校验：NOT_RUN。当前容器没有 openspec 命令，registry.npmjs.org DNS 解析未成功，未执行官方 CLI 校验。
- Java/Maven/OpenCode 本地运行测试：NOT_RUN。本次仅同步文档，未运行候选源码的构建或测试。
- 三分支同步/兼容通过：NOT_DONE；本次不更新三条 feature 分支。
- 主规范同步、归档、合并到版本分支及发布：NOT_DONE。

文档结构检查、Git 内容哈希一致性检查、CI 状态查询分别记录；它们不能相互替代，也不能代替人工批准。

## 汇总来源与不变范围

- 候选代码来源：`fix/runtime-integration-hardening@6c590c3a9af2a595d729cb6e50b1fd6d3458459e`。
- 规范来源：`docs/opencode-runtime-integration-spec@36fa7504a07fcd9dca2da7c490bcb0d696211c77`。
- 原始规范子树：`50b7eeb08425f2d4c00e0fdde4057267e0624e8e`。
- 汇总时仅更新 OpenSpec 文档及本轮状态说明；源码和测试子树保持 `7e81c66d96cbdfa7d53a5e7d80f7295caad049bf`，构建与 CI 配置不变。
- 规范任务保持未勾选；已有候选代码必须逐条关联需求、补失败用例并复验，不能追认完成。
- 纯文档分支保留原始评审快照，不删除、不移动；候选分支通过新增提交向前推进，不 reset、不强推。

## 本次操作开始时的版本分支快照

以下是读取远端所得快照，不是本次提交产物；后续操作应重新 fetch，不能用旧副本覆盖新的审计修复。

| 分支 | 读取时提交 |
|---|---|
| feature/1.0.x | cf53e19bee92981d216c2a7a800a9113ab7e6870 |
| feature/2.0.x | e85f2caf040358dfe43cf6a09fa054a059a34b92 |
| feature/3.0.x | 2e22856e47219444de5bf7ff66a09841c8c40ea0 |

这些分支已有相对于最初分析基线的新提交。真正实施和回移时需重新比较各分支差异；不能将当前候选分支视作已包含这些提交。

## 验证命令与证据规则

在项目目录中安装可用的 OpenSpec CLI 后执行，并记录工具版本、命令、退出码和完整输出：

```sh
openspec --version
openspec validate harden-opencode-runtime-integration --strict
openspec status --change harden-opencode-runtime-integration --json
```

严格校验预期退出码为 0；status 仅显示工件依赖情况，不是批准证明。Java/Maven 和真实 OpenCode 测试需分别记录实际命令、运行环境、通过/失败/跳过数与报告路径。

现有 CI 工作流的 push / pull_request 过滤目标为 feature/3.0.x，另声明 workflow_dispatch。仅推送候选分支不能假定自动触发 CI；应按最终提交 SHA 查询运行记录。历史提交 CI 成功不能记作本次新提交 CI 成功；无记录应记为 NOT_TRIGGERED，排队或执行中也不能记作通过。

## 后续实现门禁

批准人、时间、文档提交 SHA、范围与保留项必须真实记录。继续实现前读取完整 proposal、design、tasks、specs 和当前分支代码；按需求补失败测试，再执行实现及三分支验证。文档归档和发布仍受这些门禁约束。
