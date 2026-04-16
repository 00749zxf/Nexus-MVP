# Nexus电商系统Demo

基于Spring Boot + Vue.js的简化电商系统演示项目,demo版本是本地启动,然后后续版本会部署在云端环境。

## 项目结构

```
NexusDemo/
├── start.bat              # Windows批处理一键启动脚本
├── stop.bat               # Windows批处理停止脚本
├── start.ps1              # PowerShell启动脚本（推荐）
├── stop.ps1               # PowerShell停止脚本
├── start.sh               # Shell启动脚本（Git Bash/WSL）
├── stop.sh                # Shell停止脚本
├── manual-start.md        # 手动启动指南
├── nexus-backend/         # 后端模块 (Spring Boot)
├── nexus-frontend/        # 前端模块 (Vue.js)
└── README.md              # 本文件
```

## 环境要求,有就行,版本冲突了另说,没有的话安一个,别安太新的,也别安太老的,容易出bug

1. **Java Development Kit (JDK) 21+**
2. **Maven 3.6+**
3. **Node.js 18+** 和 npm
4. **MySQL 8.0+** (数据库)
5. **Redis 7.0+** (缓存，可选但推荐)

## 快速开始

### 1. 环境准备

确保已安装以下软件并配置环境变量：

- Java (运行 `java -version` 检查)
- Maven (运行 `mvn -v` 检查)  
- Node.js (运行 `node -v` 和 `npm -v` 检查)
- MySQL (运行 `mysql --version` 检查)

### 2. 数据库设置

```sql
-- 创建数据库
CREATE DATABASE nexus_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE nexus_demo;

-- 运行 schema.sql 脚本（位于 nexus-backend/src/main/resources/db/migration/）
-- 或直接导入 schema.sql 文件
```

### 3. 多种启动方式
#### 实际能用的只有手动启动,要用脚本启动你得自己捣鼓捣鼓自己配置

#### 方式一：PowerShell脚本（推荐）
```powershell
#实际能用的只有手动启动,要用脚本启动你得自己捣鼓捣鼓自己配置
# 右键点击 start.ps1 -> "使用PowerShell运行"
# 或从命令行运行：
powershell -ExecutionPolicy Bypass -File start.ps1
```

#### 方式二：Windows批处理脚本
```bash
# 双击运行 start.bat
# 或从命令行运行：
start.bat
```

#### 方式三：Shell脚本（Git Bash/WSL）
```bash
# 给脚本执行权限（首次）
chmod +x start.sh

# 运行脚本
./start.sh
```

#### 方式四：手动启动（如果脚本无法运行）
参考 [manual-start.md](manual-start.md) 文件中的详细步骤。

### 4. 停止服务

#### PowerShell方式
```powershell
powershell -ExecutionPolicy Bypass -File stop.ps1
```

#### 批处理方式
```bash
stop.bat
```

#### Shell方式
```bash
./stop.sh
```

### 5. 脚本使用说明

#### 如果批处理脚本无法运行
如果双击 `.bat` 文件没有反应，可能是以下原因：（脚本中的cd后面的要改成自己文件的目录,不改没法运行）



1. **文件关联问题**：
   - 右键点击文件 -> "打开方式" -> 选择"命令提示符"
   - 或使用PowerShell脚本（如果电脑里有的话）

2. **权限问题**：
   ```powershell
   # 以管理员身份运行PowerShell
   powershell -ExecutionPolicy Bypass -File start.ps1
   ```

3. **安全策略限制**（PowerScript）：
   ```powershell
   # 临时允许脚本执行
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   ```

#### 各脚本功能对比

| 脚本类型 | 适用环境 | 特点 |
|----------|----------|------|
| `start.ps1` | Windows PowerShell | 功能最全，推荐使用 |
| `start.bat` | Windows 命令提示符 | 传统批处理，简单 |
| `start.sh` | Git Bash/WSL/Linux/macOS | 跨平台支持 |
| 手动启动 | 任何环境 | 最灵活，适合调试 |

所有脚本都会：
1. 检查环境依赖（Java, Maven, Node.js）
2. 检查端口占用（8083, 5173）
3. 安装前端依赖（必安!）
4. 编译后端项目（必安!）
5. 启动后端和前端服务
6. 验证服务健康状态

## 服务访问地址
# (想换的话自己换)
| 服务 | 地址 | 说明 |
|------|------|------|
| 前端界面 | http://localhost:5173 | Vue.js应用 |
| 后端API | http://localhost:8083/api | Spring Boot REST API |
| API文档 | http://localhost:8083/api/swagger-ui.html | Swagger UI接口文档 |

## 核心功能模块

### 已实现
- ✅ 用户管理（注册、登录、JWT认证）
- ✅ 统一异常处理和响应封装
- ✅ Spring Security安全配置
- ✅ 数据库设计和测试数据
- ✅ 前端项目基础框架

### 待实现
- 🔄 商品管理模块
- 🔄 购物车模块  
- 🔄 订单管理模块
- 🔄 前端页面实现

## 开发说明

[/大招/]: # (脚本一个都用不了就用这个,最为原始但最好用的打开方式)
### 后端开发
```bash
cd nexus-backend
# 编译项目
mvn clean compile
# 运行测试
mvn test
# 打包
mvn clean package
```

### 前端开发
```bash
cd nexus-frontend
# 安装依赖（首次）
npm install
# 启动开发服务器
npm run dev
# 构建生产版本
npm run build
```

## 配置文件

### 后端配置 (nexus-backend/src/main/resources/application.yml)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nexus_demo
    username: "你自己的"
    password: "你自己的"
  redis:
    host: localhost
    port: 6379
server:
  port: 8083
#  有谁用谁,谁空用谁
  servlet:
    context-path: /api
```

### 前端代理配置 (nexus-frontend/vite.config.js)
```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8083',
      changeOrigin: true
    }
  }
}
```

## 数据库设计

核心表结构：

| 表名 | 说明 | 前缀 |
|------|------|------|
| ums_member | 用户表 | ums_ (会员管理) |
| pms_product | 商品表 | pms_ (商品管理) |
| oms_cart | 购物车表 | oms_ (订单管理) |
| oms_order | 订单表 | oms_ (订单管理) |
| oms_order_item | 订单项表 | oms_ (订单管理) |

## 常见问题

### 1. 端口被占用
- 修改 `nexus-backend/src/main/resources/application.yml` 中的 `server.port`
- 修改 `nexus-frontend/vite.config.js` 中的代理目标端口
- 修改 `start.bat` 和 `stop.bat` 中的端口号

### 2. 数据库连接失败
- 检查MySQL服务是否运行
- 检查数据库连接配置（用户名、密码、数据库名）
- 确认已创建 `nexus_demo` 数据库

### 3. Redis连接失败
- Redis不是必须的，如果未安装可以暂时忽略
- 如需禁用Redis，移除相关配置和依赖

### 4. 脚本无法运行
- **批处理文件双击无反应**：使用PowerShell脚本 (`start.ps1`) 或参考手动启动指南
- **PowerScript执行策略限制**：
  ```powershell
  # 临时允许脚本执行
  Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
  # 然后运行脚本
  .\start.ps1
  ```
- **权限不足**：以管理员身份运行命令提示符或PowerShell
- **文件关联错误**：右键点击 -> "打开方式" -> 选择"命令提示符"或"Windows PowerShell"

### 5. 依赖安装失败
- 检查网络连接
- 尝试使用国内镜像源（阿里云、淘宝npm镜像）
- 清除缓存重新安装

## 技术栈

### 后端
-**(有就行,能跑就可以,版本冲突了再按这个来)**
- **框架**: Spring Boot 3.2.4
- **安全**: Spring Security + JWT
- **持久层**: MyBatis
- **数据库**: MySQL 8.0
- **缓存**: Redis (可选)
- **API文档**: SpringDoc OpenAPI 3.0

### 前端  
- **框架**: Vue.js 3
- **UI组件**: Element Plus
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **HTTP客户端**: Axios
- **构建工具**: Vite

***邮箱号***：13238740650@163.com
 - **电话微信同上** 
 -   1. 补齐订单模块后端（缺失最严重）
        - 创建Order实体类、DTO、VO
        - 实现OrderService和OrderServiceImpl
        - 实现OrderController
        - 添加OrderMapper.xml
2. 完善前端页面实现
   - Home页面（首页展示）
   - ProductList/ProductDetail（商品浏览）
   - Cart（购物车交互）
   - Checkout/OrderList/OrderDetail（订单流程）
   - Profile（用户中心）
3. 前后端联调测试
   - 确保API接口对接正常
   - 测试完整购物流程
4. 可选优化
   - 商品分类功能完善
   - 订单状态流转逻辑
   - Redis缓存集成

