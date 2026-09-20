# 评审门禁与当前状态

## 当前状态

- 工件：书面评审稿；没有批准记录。
- 人工批准：NOT_APPROVED。
- 实现：PAUSED；已有代码为候选，不作为验收通过。
- OpenSpec 官方 CLI 严格校验：NOT_RUN。当前容器没有 openspec 命令，registry.npmjs.org DNS 解析失败，无法获取工具。
- Java/Maven/OpenCode 运行测试：NOT_RUN。本轮只修改文档。
- 三分支同步/兼容通过：NOT_DONE。
- 主规范同步、归档、合并及发布：NOT_DONE。

文档结构检查是独立的辅助检查；不等于官方 CLI、行为正确性或人工批准。

## 待执行命令

在本 change 所在项目目录、OpenSpec CLI 安装可用后执行，并记录具体版本：

```sh
openspec --version
openspec validate harden-opencode-runtime-integration --strict
openspec status --change harden-opencode-runtime-integration --json
```

预期严格校验退出码为 0。status 只显示工件依赖情况，不是批准证明。

## 批准记录要求

批准人、时间、文档提交 SHA、批准范围与保留项需要真实填写后才能进入实现；此处不预填用户批准。
任务完成时另外提供：实现提交、需求编号、测试命令、环境、退出码、通过/失败/跳过数量及报告位置。

## 原有实现的处理

保留 `fix/runtime-integration-hardening@6c590c3a9af2a595d729cb6e50b1fd6d3458459e`；不 reset、不强推、不合并。
按照 tasks 1.4 对照规范后决定复用或调整，不能因为已有测试文件就勾选任务。
本次修订工件在独立 `docs/opencode-runtime-integration-spec` 分支上供评审；内容不是已经落入三条 feature 分支的承诺。
