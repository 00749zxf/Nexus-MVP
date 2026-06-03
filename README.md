# Nexus电商系统

基于Spring Boot + Vue.js的电商系统，集成了DeepSeek智能客服Agent。

## 项目结构

```
Nexus-MVP/
├── .env.example            # 环境变量模板
├── nexus-backend/          # 后端模块 (Spring Boot)
├── nexus-frontend/         # 前端模块 (Vue.js)
├── start.bat               # Windows启动脚本
├── stop.bat                # Windows停止脚本
└── README.md               # 本文件
```

## 环境要求

1. **JDK 21+**
2. **Maven 3.6+**
3. **Node.js 18+**
4. **MySQL 8.0+**
5. **Redis 7.0+** (Agent对话记忆存储)
6. **PostgreSQL 16+** (RAG向量数据库，需安装pgvector扩展)

## 快速开始

### 1. 数据库设置

**MySQL (业务数据)**
```sql
CREATE DATABASE nexus_demo DEFAULT CHARACTER SET utf8mb4;
USE nexus_demo;
-- 运行 nexus-backend/src/main/resources/db/migration/schema.sql
```

**PostgreSQL (RAG向量数据库)**
```bash
# 使用Docker快速启动pgvector
docker run -d \
  --name pgvector \
  -e POSTGRES_PASSWORD=123456 \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# 创建数据库
docker exec -it pgvector psql -U postgres -c "CREATE DATABASE nexus_agent;"
```

### 2. 配置环境变量

```bash
# 复制模板
cp .env.example .env

# 编辑 .env 填入真实配置：
DB_PASSWORD=你的数据库密码
JWT_SECRET=你的JWT密钥
DEEPSEEK_API_KEY=你的DeepSeek API Key
ALIYUN_API_KEY=你的阿里云百炼API Key  # RAG向量化和重排
```

### 3. 启动服务

```bash
# 后端
cd nexus-backend && mvn spring-boot:run

# 前端
cd nexus-frontend && npm install && npm run dev
```

## 服务地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端API | http://localhost:8083/api |
| Swagger | http://localhost:8083/api/swagger-ui.html |

## 核心功能

### 已实现
- ✅ 用户管理（注册、登录、JWT认证）
- ✅ 商品/订单/购物车模块
- ✅ **智能客服Agent（DeepSeek LLM）**
- ✅ **RAG检索增强生成（pgvector + 阿里云百炼）**
- ✅ **对话记忆持久化（Redis）**
- ✅ **工具调用（商品/订单/购物车/用户查询）**
- ✅ 用户身份识别

### Agent功能
| 功能 | 说明 |
|------|------|
| 多轮对话 | Redis存储，24小时过期 |
| 工具调用 | queryProduct/queryOrder/queryCart/queryUser |
| 身份识别 | 从JWT自动获取用户信息 |
| RAG知识库 | 基于pgvector的语义检索，支持产品说明、FAQ等文档 |

### RAG检索增强生成
| 组件 | 技术栈 | 说明 |
|------|--------|------|
| 向量数据库 | PostgreSQL 16 + pgvector | 存储文本向量，HNSW索引加速检索 |
| 文本向量化 | 阿里云百炼 text-embedding-v3 | 1024维向量，中文效果优秀 |
| 语义重排 | 阿里云百炼 gte-rerank | Top-5召回 → Top-3精排 |
| 文本分块 | Spring AI TokenTextSplitter | 300 token/块，50 token重叠 |
| 检索流程 | 向量检索 → Rerank → Prompt注入 | 毫秒级响应，严格基于资料回答 |

**特性：**
- 📄 支持TXT/Markdown文档导入
- 🔍 语义检索 + 精准重排（相似度阈值0.4，相关度阈值0.3）
- 🚫 不知道就说不知道，不编造信息
- ⚡ HNSW索引，毫秒级检索响应

**RAG API：**
- POST `/api/agent/knowledge/import` - 导入知识库文档（从classpath:documents/）
- GET `/api/agent/knowledge/search?query=xxx` - 测试检索效果

### Agent API
| 接口 | 说明 |
|------|------|
| POST /api/agent/chat | Agent对话 |
| GET /api/agent/types | Agent类型列表 |
| GET /api/agent/tools | 工具列表 |

## 技术栈

### 后端
- Spring Boot 3.2.4 + Java 21
- Spring Security + JWT
- MyBatis + MySQL 8.0
- Redis (对话记忆)
- PostgreSQL + pgvector (RAG向量数据库)
- DeepSeek API (对话生成)
- 阿里云百炼 (文本向量化 + 重排)

### 前端
- Vue.js 3 + Vite
- Element Plus
- Pinia + Vue Router
- Axios

## RAG实现详解

完整的RAG技术实现文档请参考：[RAG实现详解.md](RAG实现详解.md)

### RAG架构概览

**离线数据注入（一次性）：**
```
原始文档(TXT) → 解析清洗 → 文本分块(300 token) → 向量化(1024维) → 存入pgvector + HNSW索引
```

**在线检索生成（每次请求）：**
```
用户问题 → 向量化 → pgvector检索Top-5 → Rerank重排Top-3 → 注入Prompt → DeepSeek生成答案
```

### 使用示例

1. **导入知识库文档**
```bash
# 将文档放入 nexus-backend/src/main/resources/documents/
# 支持 .txt 和 .md 文件

curl -X POST http://localhost:8083/api/agent/knowledge/import
```

2. **Agent自动调用RAG**
```bash
POST /api/agent/chat
{
  "message": "耳机保修多久？",
  "agentType": "CUSTOMER_SERVICE"
}

# Agent会自动检索知识库，基于检索结果回答
# 如果知识库中没有相关资料，会明确告知"暂无相关资料"
```

### 技术亮点

- **语义检索**：使用余弦相似度，相似度阈值0.4过滤无关结果
- **精准重排**：Rerank模型二次打分，相关度阈值0.3，只保留最相关的Top-3
- **防止幻觉**：Prompt约束AI严格基于资料回答，不编造信息
- **高性能**：HNSW索引支持毫秒级向量检索（1-10ms）
- **文本重叠**：分块时保留50 token重叠，防止关键信息被截断
