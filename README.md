# Nexus电商系统Demo

基于Spring Boot + Vue.js的简化电商系统演示项目，集成了DeepSeek智能客服Agent。

## 项目结构

```
NexusDemo/
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

## 快速开始

### 1. 数据库设置

```sql
CREATE DATABASE nexus_demo DEFAULT CHARACTER SET utf8mb4;
USE nexus_demo;
-- 运行 nexus-backend/src/main/resources/db/migration/schema.sql
```

### 2. 配置环境变量

```bash
# 复制模板
cp .env.example .env

# 编辑 .env 填入真实配置：
DB_PASSWORD=你的数据库密码
JWT_SECRET=你的JWT密钥
DEEPSEEK_API_KEY=你的DeepSeek API Key
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
- ✅ **对话记忆持久化（Redis）**
- ✅ **工具调用（商品/订单/购物车/用户查询）**
- ✅ 用户身份识别

### Agent功能
| 功能 | 说明 |
|------|------|
| 多轮对话 | Redis存储，24小时过期 |
| 工具调用 | queryProduct/queryOrder/queryCart/queryUser |
| 身份识别 | 从JWT自动获取用户信息 |
| 知识库 | 内置端口、启动方式等技术信息 |

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
- DeepSeek API

### 前端
- Vue.js 3 + Vite
- Element Plus
- Pinia + Vue Router
- Axios