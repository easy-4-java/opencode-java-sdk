# 验证与交付契约

## Purpose

使完成声明可由固定版本、真实测试和批准记录追溯。

## ADDED Requirements

### Requirement: VER-01 Review before implementation

项目 MUST 在继续业务实现前完成书面规范/计划批准，并分别记录工件齐备、格式校验和批准状态。

#### Scenario: Artifacts without approval

- **WHEN** proposal、specs、design、tasks 已存在但尚无用户批准
- **THEN** 保持待评审，不把 OpenSpec 的工件状态当作实施授权。

#### Scenario: Prior code exists

- **WHEN** 实现分支已有提前提交的代码或测试
- **THEN** 保留候选改动并逐条对照规范复验，不倒填完成标记。

### Requirement: VER-02 Pinned upstream contract

项目 MUST 记录 OpenCode tag、实际版本输出、二进制来源和摘要，并用真实解析/运行结果验证契约。

#### Scenario: Flag acceptance

- **WHEN** 验证类型化选项
- **THEN** 检查目标版本接受的 argv，echo 字符串匹配只能作为辅助单测。

#### Scenario: Missing credentials

- **WHEN** 需要凭据或付费模型的测试未执行
- **THEN** 单独标记 NOT_RUN/SKIPPED，不计入通过数量。

### Requirement: VER-03 Real process and race coverage

项目 MUST 使用真实受控子进程及确定性事件 fixture 覆盖资源、取消和并发边界。

#### Scenario: Process regression

- **WHEN** 验证执行器
- **THEN** 包括非零退出、超时、中文拆包、并发、无换行大输出、句柄关闭和进程清理。

#### Scenario: SSE regression

- **WHEN** 验证流式聊天
- **THEN** 包括就绪前取消、断连、结构化 idle、快照/增量和终态竞态。

### Requirement: VER-04 Cross branch equivalence

项目 MUST 在三条维护分支使用同一组可观察契约，兼容差异只限经记录的 Java/Jackson/构建适配。

#### Scenario: Three branch verification

- **WHEN** 宣称本次功能完成
- **THEN** 提供三条分支各自提交、JDK、命令与测试结果，不以一条线通过代替全部。

#### Scenario: Unresolved Java baseline

- **WHEN** 1.0.x 的 JDK 8 声明与 release=17 冲突未解决
- **THEN** 该分支兼容与发布门禁保持阻塞，不宣称 JDK 8 可用。

### Requirement: VER-05 Evidence based archive

项目 MUST 在严格校验、人工批准及必要测试满足后才同步主规范并归档变更。

#### Scenario: Validator unavailable

- **WHEN** OpenSpec CLI 不可用或未运行
- **THEN** 记录 NOT_RUN 和原因，不能用自写结构检查冒充官方严格校验。

#### Scenario: Incomplete implementation

- **WHEN** 仍有未验收能力或必须的测试缺失
- **THEN** 保持 change 活跃、任务未勾选，不归档为完成。
