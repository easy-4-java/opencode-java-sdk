# CLI 执行契约

## Purpose

使 Java 调用者能够限制资源、观察实时输出并可靠识别进程结果。

## ADDED Requirements

### Requirement: RUN-01 Bounded concurrency

SDK MUST 在同一执行器实例内兑现正数并发上限，并对排队、取消和超时提供有界行为。

#### Scenario: Capacity reached

- **GIVEN** 并发上限为 N 且已达到
- **WHEN** 提交下一项执行
- **THEN** 在准入成功前不启动新进程；准入超时或取消有明确结果。

#### Scenario: Release capacity

- **WHEN** 启动失败、进程退出或执行被取消
- **THEN** 资源清理完成后容量恢复且只释放一次，后续执行不永久阻塞。

### Requirement: RUN-02 Bounded capture

SDK MUST 独立限制 stdout、stderr、无换行帧与待派发数据的保留量。

#### Scenario: Output exceeds limit

- **WHEN** 输出超过配置上限
- **THEN** 保留数据不超过上限且标注截断，不能停止排空管道导致死锁。

#### Scenario: Oversized protocol frame

- **WHEN** JSON 帧或消费队列超过限制
- **THEN** 协议流明确失败或应用事先选择的策略，不静默丢失业务事件。

### Requirement: RUN-03 Per call environment

SDK SHALL 允许每次调用独立控制环境继承、覆盖、移除和工作目录，且不修改父进程环境。

#### Scenario: Concurrent contexts

- **WHEN** 两个并发调用覆盖同一个环境键为不同值
- **THEN** 各自子进程只收到自身配置，后续无覆盖调用不受污染。

#### Scenario: No inheritance

- **WHEN** 明确关闭环境继承
- **THEN** 只传递本次允许的变量，并对执行所需变量缺失返回可诊断错误。

### Requirement: RUN-04 Explicit outcomes

SDK MUST 区分成功、非零退出、启动失败、准入超时、运行超时、取消和输出处理失败，并保留已有输出及实际退出信息。

#### Scenario: Nonzero exit

- **WHEN** 子进程先输出诊断信息再以非零码退出
- **THEN** 调用者取得真实退出码及保留范围内的 stdout/stderr，不只得到 -1 和空正文。

#### Scenario: Cancel before spawn

- **WHEN** 排队中的调用被取消
- **THEN** 不启动子进程且返回取消终态。

#### Scenario: Cancel while running

- **WHEN** 运行中调用被取消或超时
- **THEN** 终止所拥有的执行并回收管道、等待任务和准入容量，原因可区分。

### Requirement: RUN-05 Incremental delivery

SDK SHALL 在进程退出前交付完整输出帧，分离协议 stdout 与诊断 stderr，并提供独立完成结果。

#### Scenario: Split UTF-8 frame

- **WHEN** 中文字符和 JSON 帧被拆分在多个读块内
- **THEN** 按顺序还原并在进程仍运行时交付，不产生乱码或重复帧。

#### Scenario: Callback fails

- **WHEN** 调用方回调抛异常
- **THEN** 该错误被明确报告，清理可控资源，不误报为正常成功或 JSON 解析错误。

#### Scenario: Preserve whitespace

- **WHEN** 捕获输出含有有效前后空白
- **THEN** 原始结果保留空白，展示层格式化不改变原始数据。
