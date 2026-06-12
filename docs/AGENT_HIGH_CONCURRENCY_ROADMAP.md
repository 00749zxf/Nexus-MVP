# Agent 高并发治理后续更新计划

本文档用于指导 Nexus MVP 的 Agent/AI 客服继续演进到可承载高并发的生产级架构。后续开发者只需要按阶段阅读和执行，不需要重新梳理整体方向。

## 0. 当前基线

当前系统已经完成第一阶段单机保护，核心代码如下：

- `nexus-backend/src/main/java/com/nexus/agent/controller/AgentController.java`
- `nexus-backend/src/main/java/com/nexus/agent/protection/AgentConcurrencyGuard.java`
- `nexus-backend/src/main/java/com/nexus/agent/protection/AgentProtectionProperties.java`
- `nexus-backend/src/main/java/com/nexus/agent/core/AgentResponse.java`
- `nexus-backend/src/test/java/com/nexus/agent/protection/AgentConcurrencyGuardTest.java`

当前能力：

- `/agent/chat` 入口已接入并发保护。
- 单实例内通过 `Semaphore` 控制同时进入 LLM 链路的请求数量。
- 同一用户短时间重复请求会被限流。
- LLM 请求超时会降级返回。
- 连续失败后会熔断，避免持续打爆 LLM 服务。
- `/agent/protection/status` 可以查看保护层状态。

当前限制：

- 限流和熔断状态保存在单个 JVM 内存中，多实例部署后不能共享。
- `/agent/chat` 仍然是同步接口，用户请求线程会等待 LLM 返回。
- 高峰期多余请求会快速降级，而不是排队后异步返回完整 AI 回复。
- 没有统一指标系统，无法长期观察 QPS、P95、错误率、熔断次数。
- 没有真实的 Agent 高并发压测脚本和容量基准。

当前结论：

> 当前版本已经能在高并发下保护系统不被拖死，但还不是“几千人同时完整获得 AI 回复”的最终架构。最终目标需要分布式限流、异步任务队列、Worker 扩容、实时推送和压测闭环。

## 1. 阶段一：巩固单机保护层

状态：已完成主体实现，后续只做增强。

目标：

- 保证单实例在突发流量下不会无限堆积请求。
- 保证 LLM 慢调用、异常、超时不会拖死 Tomcat、数据库连接池和 Redis。
- 为下一阶段分布式化保留清晰接口。

已完成内容：

- 新增 `AgentConcurrencyGuard`。
- 新增 `AgentProtectionProperties`。
- `/agent/chat` 包装为受保护调用。
- 新增 `degraded` 和 `degradeReason` 响应字段。
- 新增 `AgentConcurrencyGuardTest`。

建议补充：

- 给 `/agent/protection/status` 加权限控制，只允许管理员访问。
- 前端识别 `degraded=true`，展示“繁忙、限流、超时、熔断”等更友好的提示。
- 将保护层日志从普通日志升级为结构化日志，字段至少包含 `sessionId`、`callerKey`、`degradeReason`、`responseTime`。

验收标准：

- `mvn -pl nexus-backend test` 通过。
- 连续快速请求同一用户时，能看到 `RATE_LIMITED`。
- 把 `max-concurrent-requests` 调成 `1` 后，同时发起多个请求，后续请求能快速得到 `SERVER_BUSY`。
- 模拟 LLM 失败后，连续失败达到阈值能得到 `CIRCUIT_OPEN`。

## 2. 阶段二：Redis 分布式限流和熔断

目标：

- 支持后端多实例部署。
- 让限流、熔断、计数状态不再局限于单 JVM。
- 避免多个实例各放行一批请求，最终叠加打爆 LLM。

需要新增或修改的模块：

- 新增 `AgentDistributedRateLimiter`。
- 新增 `AgentDistributedCircuitBreaker`。
- 扩展 `AgentConcurrencyGuard`，让它可以选择本地模式或 Redis 模式。
- 配置文件新增：

```yaml
nexus:
  agent:
    protection:
      mode: redis
      global-qps: 50
      user-qps: 1
      circuit-window-seconds: 30
      circuit-failure-threshold: 20
```

实现建议：

- Redis 限流使用 Lua 脚本保证原子性。
- 全局限流 key 示例：`nexus:agent:rate:global:{秒级时间窗}`。
- 用户限流 key 示例：`nexus:agent:rate:user:{userId}:{秒级时间窗}`。
- 熔断 key 示例：`nexus:agent:circuit:global`。
- 失败计数 key 示例：`nexus:agent:circuit:failures:{时间窗}`。

关键代码思路：

```text
请求进入 /agent/chat
  -> 本地舱壁检查
  -> Redis 全局限流
  -> Redis 用户限流
  -> Redis 熔断检查
  -> 执行或降级
```

验收标准：

- 启动两个后端实例，配置不同端口。
- 用 Nginx 或压测脚本轮询两个实例。
- 总放行量不能超过 Redis 中配置的全局阈值。
- 任意一个实例触发熔断后，其他实例也应该读到熔断状态。

风险点：

- Redis 不可用时必须有本地兜底策略，不能让 Agent 接口整体不可用。
- Lua 脚本要有过期时间，避免 key 无限增长。
- 分布式限流的时间窗会有边界抖动，后续可从固定窗口升级到滑动窗口或令牌桶。

## 3. 阶段三：同步接口改为异步任务模型

目标：

- 几千用户同时提问时，请求线程不直接等待 LLM。
- `/agent/chat` 只负责提交任务，真正的 LLM 调用交给后台 Worker。
- 用户可以通过轮询、SSE 或 WebSocket 拿到最终回答。

推荐接口设计：

```text
POST /api/agent/chat
  -> 返回 taskId、sessionId、status=QUEUED

GET /api/agent/chat/tasks/{taskId}
  -> 查询任务状态和结果

GET /api/agent/chat/stream/{taskId}
  -> SSE 流式接收结果
```

任务状态建议：

```text
QUEUED
RUNNING
SUCCEEDED
FAILED
TIMEOUT
CANCELLED
DEGRADED
```

需要新增的数据表：

```sql
CREATE TABLE agent_chat_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL UNIQUE,
  session_id VARCHAR(64) NOT NULL,
  member_id BIGINT NULL,
  username VARCHAR(64) NULL,
  message TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  response TEXT NULL,
  degrade_reason VARCHAR(64) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  finished_at DATETIME NULL,
  INDEX idx_session_id (session_id),
  INDEX idx_member_id (member_id),
  INDEX idx_status_created_at (status, created_at)
);
```

需要新增或修改的模块：

- `AgentTaskController`
- `AgentTaskService`
- `AgentTaskMapper`
- `AgentTask`
- `AgentTaskStatus`
- 前端 `agentApi.js`
- 前端 Agent store

第一版可以先用数据库表作为任务队列。

请求流程：

```text
用户提交问题
  -> 创建 agent_chat_task
  -> 返回 taskId
  -> 前端轮询 taskId
  -> 后台定时任务扫描 QUEUED
  -> 执行 AgentEngine
  -> 写入结果
  -> 前端展示结果
```

验收标准：

- 1000 个请求同时提交时，HTTP 请求能快速返回 taskId。
- Tomcat 线程不被 LLM 调用长期占用。
- 后台 Worker 按配置并发数慢慢消费任务。
- 任务结果可查询，可追踪失败原因。

风险点：

- 只用数据库做队列，吞吐有限，但适合 MVP 过渡。
- 要加任务超时和最大重试次数，避免坏任务永久卡住。
- 同一个 session 的消息最好按顺序处理，否则上下文可能乱序。

## 4. 阶段四：引入 MQ 和 Agent Worker

目标：

- 从“数据库轮询任务”升级到真正的消息队列。
- Agent 后端 API 和 LLM Worker 解耦。
- Worker 可以独立水平扩容。

推荐技术选型：

- 简单场景：Redis Stream
- 业务可靠性优先：RabbitMQ
- 大规模日志和事件流：Kafka

对当前项目推荐：

> 优先使用 Redis Stream 或 RabbitMQ。Nexus 当前已有 Redis，Redis Stream 接入成本低；如果面试或生产表达更稳，RabbitMQ 的任务队列语义更清晰。

目标架构：

```text
前端
  -> 后端 API: 提交 Agent 任务
  -> MySQL: 保存任务
  -> MQ: 投递 taskId
  -> Agent Worker: 消费 taskId
  -> LLM/RAG/Tools
  -> MySQL/Redis: 保存结果
  -> SSE/WebSocket: 推送给前端
```

需要新增或修改的模块：

- `AgentTaskProducer`
- `AgentTaskConsumer`
- `AgentWorkerService`
- `AgentTaskRetryPolicy`
- `AgentTaskDeadLetterHandler`

Worker 并发策略：

- 每个 Worker 实例配置 `worker-concurrency`。
- 全局 LLM 配额仍由 Redis 限流控制。
- 单 session 使用顺序锁，避免同一会话消息乱序。

验收标准：

- API 实例和 Worker 实例可以独立启动。
- Worker 停止时，任务不会丢失。
- Worker 重启后，可以继续消费未完成任务。
- LLM 失败后能按策略重试。
- 多次失败进入死信队列或 `FAILED` 状态。

风险点：

- 要处理消息重复投递，任务执行必须幂等。
- 要防止同一个 taskId 被多个 Worker 同时处理。
- 要有死信机制，否则异常任务会拖慢整个队列。

## 5. 阶段五：SSE/WebSocket 实时返回

目标：

- 用户提交问题后不需要一直阻塞等待。
- AI 结果生成后实时推送到前端。
- 后续可以支持流式输出。

建议优先级：

1. 先做轮询，最简单稳定。
2. 再做 SSE，适合服务端向客户端单向推送。
3. 最后视需求做 WebSocket，适合复杂双向交互。

推荐第一版接口：

```text
GET /api/agent/chat/tasks/{taskId}
GET /api/agent/chat/tasks/{taskId}/events
```

SSE 事件类型：

```text
queued
running
delta
completed
failed
timeout
degraded
```

前端需要修改：

- `nexus-frontend/src/agent/api/agentApi.js`
- `nexus-frontend/src/agent/store/agent.js`
- `nexus-frontend/src/agent/components/AgentChat.vue`

验收标准：

- 用户提交后立即看到排队状态。
- Worker 开始处理时，前端显示“正在思考”。
- 完成后自动显示回答，不需要刷新页面。
- 断线后可通过 taskId 查询最终结果。

风险点：

- SSE 长连接也会占用服务资源，需要限制连接数。
- 多实例部署时，SSE 连接和 Worker 不一定在同一台机器，需要 Redis Pub/Sub 或消息广播。
- 前端要处理重复事件和断线重连。

## 6. 阶段六：监控、告警和容量基准

目标：

- 不再靠感觉判断“能不能抗住”。
- 用指标证明系统容量。
- 每次优化后都能比较压测数据。

需要接入：

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana

核心指标：

```text
agent.chat.request.count
agent.chat.accepted.count
agent.chat.rejected.rate_limit.count
agent.chat.rejected.busy.count
agent.chat.timeout.count
agent.chat.circuit.open.count
agent.chat.response.time
agent.task.queue.size
agent.task.queue.wait.time
agent.worker.running.count
agent.worker.success.count
agent.worker.failure.count
agent.llm.latency
agent.llm.error.rate
```

推荐看板：

- Agent QPS
- Agent P50/P95/P99 延迟
- 当前队列长度
- Worker 消费速率
- LLM 成功率/失败率
- 降级率
- 熔断次数
- Redis/MySQL 连接池使用率
- JVM CPU/内存/GC

告警建议：

- 降级率 5 分钟内超过 20%。
- 熔断连续发生。
- 队列积压超过阈值。
- P95 延迟超过业务目标。
- Worker 消费速率低于任务进入速率。
- Redis 或 MySQL 连接池耗尽。

验收标准：

- 压测时可以实时看到 QPS、延迟、错误率、队列长度。
- 每轮优化都有压测报告。
- 能根据指标判断瓶颈是 LLM、数据库、Redis、Tomcat 还是 Worker。

## 7. 阶段七：压测体系

目标：

- 建立稳定、可重复的压测方法。
- 明确当前系统能承载多少用户、多少 QPS、多少并发任务。

推荐工具：

- 本地快速验证：PowerShell + JMeter
- 工程化压测：k6
- Java 生态：Gatling

压测场景：

1. 单用户连续快速提问，验证限流。
2. 100 用户同时提问，验证基础稳定性。
3. 1000 用户同时提交任务，验证 API 快速返回。
4. 3000 用户同时提交任务，验证队列积压和 Worker 消费。
5. LLM 故障模拟，验证熔断和降级。
6. Redis 故障模拟，验证本地兜底。
7. Worker 停止和恢复，验证任务不丢失。

每次压测必须记录：

```text
测试日期
代码版本/commit
部署拓扑
后端实例数
Worker 实例数
数据库配置
Redis 配置
LLM 模型和配额
并发用户数
请求总量
成功率
降级率
超时率
P50/P95/P99
最大队列长度
平均队列等待时间
CPU/内存/GC
结论和下一步
```

验收标准：

- 形成 `docs/performance-reports/` 目录。
- 每次压测生成一份报告。
- 不能只说“感觉不卡”，必须用数据说话。

## 8. 最终目标架构

最终系统应该变成：

```text
Frontend
  -> Backend API Cluster
  -> Redis Distributed Rate Limit
  -> MySQL Agent Task Table
  -> MQ
  -> Agent Worker Cluster
  -> RAG / Tools / LLM
  -> Result Store
  -> SSE or WebSocket Push
  -> Observability: Prometheus + Grafana + Logs
```

最终能力目标：

- 几千用户同时提交问题时，API 仍能快速响应。
- LLM 调用由 Worker 平滑消费，不压垮 Web 线程。
- 高峰期允许排队，但队列长度和等待时间可观测。
- 单个用户刷请求不会影响全局。
- LLM 故障时自动熔断和降级。
- Redis、MySQL、Worker 异常时有明确兜底策略。
- 每次容量提升都有压测报告支撑。

## 9. 推荐开发顺序

按下面顺序推进，不建议跳阶段：

1. 完善当前单机保护层和前端降级展示。
2. 增加 Redis 分布式限流和分布式熔断。
3. 增加 Agent 任务表，把同步调用改为提交任务。
4. 第一版用数据库轮询 Worker 消费任务。
5. 引入 MQ，替代数据库轮询。
6. 引入 SSE，让结果实时返回。
7. 接入 Prometheus/Grafana。
8. 补齐 k6/JMeter 压测脚本。
9. 做多实例压测，形成容量报告。
10. 根据压测结果调优数据库、Redis、Worker、LLM 配额。

## 10. 面试回答模板

可以这样回答：

> 我会分阶段治理 AI 客服高并发。第一阶段先在同步入口做舱壁、限流、超时、熔断，保证高峰期系统不会被 LLM 慢调用拖死。第二阶段把限流和熔断迁移到 Redis，支持多实例部署。第三阶段把同步接口改成异步任务模型，用户请求只提交任务，LLM 调用由 Worker 消费。第四阶段引入 MQ 和 Worker 集群，支持水平扩容和失败重试。第五阶段通过 SSE 或 WebSocket 把结果实时推送给前端。最后接入 Prometheus、Grafana 和压测体系，用 QPS、P95、队列长度、降级率、熔断次数来证明系统容量。

核心原则：

> 高并发不是让所有请求同时执行，而是控制入口、削峰填谷、隔离慢资源、快速失败、异步处理、可观测、可压测。
