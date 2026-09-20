# 技术设计（评审稿）

## Context

目标是可靠的 Java 接入层，不是再次铺开命令列表。原始三个版本线与提前实现分支必须分开：后者已经存在代码和测试文件，但没有本轮执行证据。详见 evidence.md。

## Goals / Non-Goals

实现 proposal 的六项能力并保证同一可观察契约在三线成立。完整 ACP 协议客户端、内嵌 PTY 和 Web UI 不在范围；环境隔离不等于租户安全边界。

## Approaches

A. 只补 CLI 参数：改动小，但不能解决生命周期和流式可靠性，拒绝作为完整方案。
B. 保留现有门面并增加执行模式、生命周期与事件归约层：兼容成本可控，作为本稿建议。
C. 改成全新多模块 SDK：范围和迁移成本过大，暂不采用。
下列决策均为待批准的设计，不追认既有代码为正确实现。

## Decisions

### D1. CLI 契约与执行模式分离

`OpenCodeRunOptions`、`OpenCodeTuiOptions` 只负责经过版本核对的 argv；全局选项与 SDK 日志分开。
候选补齐项：run 的 auto/interactive；TUI 的 auto/mdns/mdns-domain/cors；serve 的 mdns-domain；ACP 的网络选项；全局 print-logs/log-level/pure；stats 的“省略/全部/前 N 个”模型统计。每项先核对目标 tag 的命令解析器和帮助输出，不把滚动网页当成固定版本证明。
`share(boolean)` 为新入口；废弃但保留 `share(String)`，null/"false" 不启用、"true" 启用，忽略大小写和首尾空白，其他值抛参数错误；不得静默把 "org" 变为 true。
CAPTURE 适用于普通命令；STREAM 适用于增量输出；INHERIT_TERMINAL 只在调用方提供可用终端时使用；MANAGED 适用于常驻服务。交互选项不得在无 stdin 的捕获模式下悄悄执行。
保留旧方法签名；新的重载和句柄由调用者显式采用。捕获结果保留原始 UTF-8 空白，展示层自行 trim，并说明 2/3 线此前 trim 的行为变化。

### D2. 资源与准入

`OpenCodeCliExecutor` 的每个实例管理自己的并发额度；同一实例的同步、流式、managed 调用共用额度。不承诺 JVM 或集群全局限流。
提供有界等待的准入超时；排队取消不启动进程。额度从准入成功持有到进程、管道和监听资源完成清理，所有终态最多释放一次。
`OpenCodeCliExecutionContext` 在调用开始时复制环境与目录；允许不继承环境、覆盖或移除变量，不修改 JVM 环境。可执行文件路径是一个独立 argv 元素，不解析为 shell 命令。
拟定默认值供评审：普通命令沿用 300 秒执行超时；准入等待 30 秒；stdout/stderr 各保留最多 1 MiB；managed 使用同上限日志尾部；单帧默认最大 256 KiB、派发队列最大 4 MiB。限制可配置但必须可验证，不能用无限队列规避溢出。
短命令保留前缀并记录截断字节；managed 保留尾部；持续排空两条管道避免死锁。CLI 协议帧超限或消费队列超限默认显式失败，不静默丢事件；日志可截断并标记。

### D3. 结果与流式句柄

建议复用并扩展现有 `OpenCodeCliResult`，增加结果分类、实际退出码（存在时）、错误原因及截断元数据；不得把全部异常折叠为 -1 和空 stdout。
`OpenCodeCliStreamHandle` 暴露增量 stdout/stderr、独立退出完成、取消与幂等 close。`runStream` 在此之上进行版本受控 JSON 帧解析。UTF-8 解码器跨块保留状态；无换行超长输出同样受限。
用户回调在受限派发边界执行；回调抛错与 JSON 解码失败明确区分。主动取消、超时和进程自行失败保留不同原因；最终结果要等待可控范围内的管道排空和清理。

### D4. Web/Serve 生命周期及安全

建议新增 `OpenCodeServerHandle`：STARTING → READY → STOPPING → EXITED；启动或运行失败进入 FAILED。记录退出事实和失败原因，不能把 spawn 成功直接当成 READY。
启动超时 30 秒、关闭宽限 5 秒是本稿默认建议；存活时间默认无上限，不继承 300 秒普通命令超时。
首版 managed API 要求明确、非零端口；没有已验证机器可读地址发现契约前不支持随机端口。CLI raw/原有 web 调用不因此删除。IPv6 地址使用合法 URI 表达，监听地址与客户端访问地址分开。
就绪必须与所启动子进程关联：启动前发现端口已被占用就失败，不把旧服务的健康返回当成本次成功；就绪时同时检查子进程仍存活、预期地址及身份/版本信息。健康探测认证与服务端启动认证分别传递。
默认 loopback。非 loopback/mDNS 要求显式开放和认证；确需无认证局域网模式时另外显式选择不安全策略并提示风险。CORS 不代替认证。日志不输出密码、token、完整环境或内联配置；子进程原始输出本身可能包含敏感信息，交付调用者的数据与 SDK 诊断日志使用不同策略。
SDK 自建进程由句柄关闭；外部连接句柄关闭只能释放客户端资源。启动失败要回收自建进程；优雅退出超时后按明确策略强制终止。JDK 8 的进程树清理必须单独验证，不能把 17/21 的 ProcessHandle 直接复制过去。

### D5. ACP 与终端边界

ACP launcher 只承诺双向 stdio、独立 stderr、进程状态和关闭；不把 stdout 当日志打印或提前全部消费。进程 STARTED 不等于 ACP 协议初始化 READY；握手由外部协议客户端负责。
完整 JSON-RPC/ACP 请求关联与会话管理、内嵌 PTY/窗口 resize 分别另提变更。本次允许继承调用方终端，但不得声称“支持 interactive flag”就已经提供完整终端产品。

### D6. SSE 聊天状态机

流程：校验请求 → 解析会话 → 建立订阅 → 等待就绪 → 提交 prompt_async → 消费当前请求事件 → 完成/失败/取消 → 清理。
连接阶段和生成阶段分别有界；建立会话失败、调度器拒绝任务、subscribe 同步异常也必须完成返回 Future。订阅就绪至少绑定 onOpen，并用固定版本真实首片测试核实服务端监听已经建立；如果上游需要 server.connected 等握手，纳入版本适配器，不能仅用“创建了句柄”作保证。
`message.part.delta` 按 session/message/part/field 归约；只向正文回调发送正文增量。`message.part.updated` 更新快照状态，不重复追加。优先精确事件类型匹配，版本事件别名由适配器处理。status 同时按目标契约读取对象中的 type，不把 Map.toString 当状态枚举。
同一客户端同一会话只允许一个高层生成；再次提交明确忙错误。外部并发不能靠本地锁解决，必须使用消息/请求关联；不能关联时要求专用会话，不能把其他客户端事件混入答案。
终态错误与 EOF 必须区分：生成中无正常终态的 EOF 视为失败；正常完成后的断连不覆盖成功。仅靠计时器兜底不算错误传播。
取消默认只取消本地订阅与尚未发送的请求；不自动 abort 共享服务端会话。显式 abort 是另一个操作。所有竞争终态只完成一次；完成后不再回调，不遗留 timeout、订阅或持有内容的监听器。
不自动重发 prompt；断线后不承诺没有服务端证据的精确一次恢复。delta/快照去重与传输级重放不是一回事。

### D7. 配置往返

`OpenCodeConfig.server` 使用可扩展的 `OpenCodeServerConfig`；根和 server 内未知字段保留原结构。Jackson 2/3 分支分别实现正确 any-setter/any-getter，不能把 JSON 键改名为 extra 或让未知字段覆盖已知字段。
读取整个对象后序列化用于往返测试，不代表应当自动 PATCH 全部配置。保留动态 Object 提交；未设置、显式 null、空值和删除按固定上游 PATCH 契约区分。已有 agent/provider 的对象形状也纳入 fixture，不能只测新增 server 字段。
注入资源保持外部所有权；SDK 自建资源失败时回收。单 HTTP/CLI 构造器的实际启用行为与文档冲突需要复现，若修复另补 delta 后再编码，不趁本提案无规格扩张。

## Migration / Rollback

按 cli-contract → cli-runtime → server-lifecycle、chat-streaming、configuration-model → contract-verification 的依赖分批实施；SSE 与配置可以独立测试。先验证 3.0.x，再按同一语义移植到 2.0.x、1.0.x。
不直接合并整个已实现分支：先把既有改动逐条关联规范、检查旧错误测试（例如 --share org），补红绿测试后再按功能单元接纳。
回滚使用明确的 revert 提交而非 reset/force-push；不删除调用方配置、不隐式重启外部服务。不得因回滚重新引入默认自动批准权限。

## Validation

测试分为纯 argv、真实 Java 子进程、MockWebServer/SSE、固定 OpenCode 二进制、跨 OS/JDK/Jackson 矩阵。无模型凭据的测试默认运行；需要模型或付费调用的测试显式开启，SKIPPED 必须独立报告。
OpenSpec CLI 的严格校验与本地结构检查是不同证据；命令和状态记录在 review.md。验收场景及任务映射见 tasks.md。

## Open Questions / Blocking Gates

- 用户尚未批准本书面稿与任务计划：阻塞所有新业务代码实现。
- 1.0.x 的 Java 8 声明/release 17 冲突：阻塞该分支移植与发布；需要另行批准基线决定并实测依赖字节码。
- 候选 CLI flag 的固定版本接受性、OpenSpec CLI 严格校验和真实 OpenCode 运行未完成：阻塞对应完成声明。
- 完整 ACP/PTY 已明确排除，不再作为本次“待实现但默认承诺”的隐含范围。
