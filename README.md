# Nexus MVP 电商平台

Nexus MVP 是一个基于 Spring Boot + Vue 3 的电商平台示例项目，集成了用户、商品、购物车、订单、智能客服 Agent、RAG 知识库、Redis 会话记忆和高并发保护能力。

## 项目结构

```text
Nexus-MVP/
├── nexus-backend/          # Spring Boot 后端，默认端口 8083
├── nexus-frontend/         # Vue 3 前端，默认端口 5174 或 Vite 自动分配端口
├── docs/                   # 架构、压测和后续演进文档
├── scripts/                # 文档生成等辅助脚本
├── manual-start.md         # 手动启动说明
├── QUICK-START.txt         # 快速启动提示
└── README.md
```

## 技术栈

后端：

- Java 21
- Spring Boot 3.2.4
- Spring Security + JWT
- MyBatis
- MySQL 8
- Redis
- PostgreSQL + pgvector
- Spring AI
- DeepSeek Chat API
- 阿里云百炼 Embedding/Rerank

前端：

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios

## 核心功能

### 电商业务

- 用户注册、登录、JWT 鉴权
- 当前用户资料查询和更新
- 管理员用户列表查询
- 商品查询、搜索、精选商品
- 购物车添加、更新、删除、批量选择
- 订单创建、支付、取消、确认收货、删除
- 地址管理和默认地址设置

### 智能客服 Agent

- `/api/agent/chat` 智能客服对话
- 支持 `CUSTOMER_SERVICE` 等 Agent 类型
- 自动从 JWT 补全当前用户身份
- 支持工具调用：
  - `queryProduct`
  - `queryOrder`
  - `queryCart`
  - `queryUser`
- 支持 Redis 会话记忆持久化
- 服务重启后可从持久化会话恢复最近上下文
- 支持 RAG 知识库检索增强生成

### RAG 知识库

- PostgreSQL + pgvector 存储向量
- HNSW 索引加速向量检索
- 阿里云百炼 `text-embedding-v3` 生成文本向量
- Rerank 服务对候选结果二次排序
- Prompt 严格约束 AI 基于知识库资料回答，减少幻觉

参考文档：

- [RAG实现详解.md](RAG实现详解.md)

## 高并发加固

当前版本已经完成第一阶段高并发治理，目标是让系统在突发流量下可控降级，而不是被慢 LLM 调用拖死。

### 已完成能力

- Agent 入口并发舱壁
- 单用户请求频率限制
- LLM 请求超时降级
- 连续失败熔断
- 降级原因返回
- Agent 保护状态查询
- 购物车并发添加防重复
- 下单扣库存原子化，防止超卖
- 订单状态流转原子化，防止重复支付/取消/确认
- 默认地址设置单语句化，避免多个默认地址
- 登录接口改为 JSON 请求体，避免用户名密码出现在 URL
- 用户更新接口使用 DTO 白名单，避免越权修改敏感字段
- `/agent/**` 从匿名放行中移除，需要认证访问

### Agent 保护配置

配置位置：

```yaml
nexus:
  agent:
    protection:
      enabled: true
      max-concurrent-requests: 20
      max-wait-ms: 200
      request-timeout-ms: 30000
      per-user-min-interval-ms: 800
      circuit-failure-threshold: 5
      circuit-open-ms: 30000
      max-throttle-keys: 10000
      throttle-key-ttl-ms: 60000
```

含义：

- `max-concurrent-requests`: 单实例同时进入 Agent/LLM 链路的最大请求数。
- `max-wait-ms`: 请求等待并发许可的最长时间，超过后快速降级。
- `request-timeout-ms`: Agent 执行超时时间。
- `per-user-min-interval-ms`: 同一用户最小请求间隔。
- `circuit-failure-threshold`: 连续失败多少次后熔断。
- `circuit-open-ms`: 熔断打开持续时间。

保护状态接口：

```http
GET /api/agent/protection/status
```

返回内容包含：

- `inFlightRequests`
- `availablePermits`
- `circuitOpen`
- `consecutiveFailures`
- `accepted`
- `succeeded`
- `failed`
- `timedOut`
- `rejectedByRateLimit`
- `rejectedByBusy`
- `rejectedByCircuit`

### 当前并发能力边界

当前阶段的并发目标是“保护系统不崩”，不是让几千个请求同时完整执行 LLM。

几千请求同时进入时，当前系统会：

```text
大量用户请求
  -> Agent 入口保护层
  -> 少量请求进入 LLM
  -> 多余请求快速限流/繁忙/熔断降级
  -> 系统保持可用
```

后续要做到“几千用户同时完整获得 AI 回复”，需要继续演进为：

- Redis 分布式限流
- 异步任务表
- MQ 削峰
- Agent Worker 集群
- SSE/WebSocket 实时返回
- Prometheus/Grafana 监控
- k6/JMeter/Gatling 压测体系

完整路线图：

- [docs/AGENT_HIGH_CONCURRENCY_ROADMAP.md](docs/AGENT_HIGH_CONCURRENCY_ROADMAP.md)

## 环境要求

- JDK 21+
- Maven 3.6+
- Node.js 18+
- MySQL 8+
- Redis 7+
- PostgreSQL 16 + pgvector
- DeepSeek API Key
- 阿里云百炼 API Key

## 数据库准备

### MySQL

```sql
CREATE DATABASE nexus_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

导入业务表结构：

```text
nexus-backend/src/main/resources/db/migration/schema.sql
```

### PostgreSQL + pgvector

使用 Docker 启动：

```powershell
docker run -d --name pgvector -e POSTGRES_PASSWORD=123456 -p 5432:5432 pgvector/pgvector:pg16
docker exec -it pgvector psql -U postgres -c "CREATE DATABASE nexus_agent;"
```

如果容器已存在：

```powershell
docker start pgvector
```

Agent/RAG 相关表：

```text
nexus-backend/src/main/resources/db/migration/agent_tables.sql
```

## 配置

不要把真实密钥提交到 GitHub。

后端本地配置文件：

```text
nexus-backend/src/main/resources/application.yml
```

示例配置文件：

```text
nexus-backend/src/main/resources/application.yml.example
```

建议通过环境变量配置敏感信息：

```powershell
$env:DB_PASSWORD="你的数据库密码"
$env:JWT_SECRET="你的JWT密钥"
$env:DEEPSEEK_API_KEY="你的DeepSeek API Key"
$env:ALIYUN_API_KEY="你的阿里云百炼 API Key"
```

`.gitignore` 已忽略：

- `.env`
- `*.env`
- `application.yml`
- `application-local.yml`
- `secrets/`
- `credentials.json`

## 启动后端

```powershell
cd E:\project\exp\Nexus-MVP
mvn -pl nexus-backend spring-boot:run
```

默认地址：

```text
http://localhost:8083/api
```

Swagger：

```text
http://localhost:8083/api/swagger-ui.html
```

如果端口 `8083` 被占用：

```powershell
netstat -ano | findstr :8083
Stop-Process -Id <PID>
```

## 启动前端

```powershell
cd E:\project\exp\Nexus-MVP\nexus-frontend
npm install
npm run dev
```

默认地址通常是：

```text
http://localhost:5174
```

如果端口被占用，Vite 会自动切换到下一个可用端口。

## 常用 API

### 用户

```http
POST /api/members/register
POST /api/members/login
GET  /api/members/me
PUT  /api/members/{id}
```

### 商品

```http
GET /api/products
GET /api/products/{id}
GET /api/products/search?keyword=xxx
```

### 购物车

```http
GET    /api/cart
POST   /api/cart
PUT    /api/cart/{itemId}
DELETE /api/cart/{itemId}
```

### 订单

```http
POST /api/orders/cart
POST /api/orders/direct
GET  /api/orders
GET  /api/orders/{orderId}
POST /api/orders/{orderId}/pay
POST /api/orders/{orderId}/cancel
POST /api/orders/{orderId}/confirm
```

### Agent

```http
POST /api/agent/chat
GET  /api/agent/types
GET  /api/agent/tools
GET  /api/agent/protection/status
```

Agent 请求示例：

```json
{
  "sessionId": "demo-session-1",
  "agentType": "CUSTOMER_SERVICE",
  "message": "我想查询一下我的订单",
  "context": {
    "currentPage": "/orders"
  }
}
```

## 测试

运行后端测试：

```powershell
mvn -pl nexus-backend test
```

当前包含：

- Agent 并发保护单元测试
- 购物车并发添加集成测试
- 下单防超卖集成测试

真实数据库并发集成测试默认跳过，需要显式开启：

```powershell
mvn -pl nexus-backend test "-Dnexus.integration.concurrency=true"
```

注意：

- 开启真实并发集成测试前，需要 MySQL、Redis、PostgreSQL 环境可用。
- 测试会创建带 `concurrency_` 前缀的数据，并在结束后清理。

## 压测建议

当前可先压测同步 Agent 保护层：

1. 把 `max-concurrent-requests` 临时调低到 `1` 或 `2`。
2. 使用 JMeter/k6 同时请求 `/api/agent/chat`。
3. 观察 `/api/agent/protection/status`。
4. 验证 `rejectedByBusy`、`rejectedByRateLimit`、`timedOut` 是否符合预期。

正式压测应记录：

- 并发用户数
- 总请求数
- 成功率
- 降级率
- 超时率
- P50/P95/P99
- 队列长度
- CPU/内存/GC
- MySQL/Redis 连接池状态

## 安全注意事项

- 不要提交真实 API Key。
- 不要提交真实 `application.yml`。
- 不要把用户名密码放在 URL query 参数里。
- `/agent/**` 应保持认证访问。
- 管理接口应限制管理员角色访问。
- 高并发场景下优先做限流、熔断和异步削峰，不要无限增加线程。

## 后续计划

优先级从高到低：

1. 给 `/agent/protection/status` 增加管理员权限。
2. 前端识别 `degraded=true`，展示更友好的繁忙/限流/熔断提示。
3. 引入 Redis 分布式限流和分布式熔断。
4. 把 `/agent/chat` 改造成异步任务提交。
5. 引入 MQ 和 Agent Worker。
6. 增加 SSE/WebSocket 实时返回。
7. 接入 Prometheus/Grafana。
8. 补齐 k6/JMeter 压测脚本和压测报告。
