# OpenSpec 工作约定

本仓库采用 `spec-driven`：proposal → specs → design → tasks → review → implementation → verification → archive。
OpenSpec 的工件依赖判断只证明文件是否齐备，不代表用户批准；本项目另外使用 change 中的 `review.md` 记录批准状态。

## 执行门禁

1. 读取 `config.yaml`、目标 change 全部工件和相关主规范；没有主规范时不得凭空使用 MODIFIED。
2. 将可观察契约写入 `specs/<capability>/spec.md`，技术选择写入 design，任务和验证写入 tasks。
3. 在项目目录执行 `openspec validate <change-id> --strict`，保存工具版本、命令、退出码与完整输出；失败或未执行都不是通过。
4. 用户明确批准书面范围与计划后才能进入实现。此前只允许只读调查和文档修改。
5. 已有提前实现的代码保留在原分支，逐条映射规范、补失败用例并复验；不能通过倒填勾选框追认完成。
6. 三条兼容线分别记录提交和测试证据。只允许 Java/Jackson/构建适配差异，不允许悄悄删功能。
7. 所有实现与验证门禁满足后，再同步主规范并归档。不得将提案直接复制成“已经实现”的主规范。

## 本轮状态

当前 change：`harden-opencode-runtime-integration`。
纯文档评审分支与 `fix/runtime-integration-hardening` 分开；不强推、不回滚、不合并现有实现分支。
正式 CLI 校验或运行测试受环境限制时，准确记录 `NOT_RUN` 和原因，保留未勾选任务。
