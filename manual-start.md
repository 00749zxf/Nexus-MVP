# Nexus电商系统Demo - 手动启动指南

如果脚本无法运行，请按照以下步骤手动启动服务。

## 环境要求
- Java Development Kit (JDK) 21+
- Maven 3.6+
- Node.js 18+ 和 npm
- MySQL 8.0+ (数据库)
- Redis 7.0+ (缓存，可选)

## 准备工作

### 1. 启动MySQL服务
确保MySQL服务正在运行：
```bash
# Windows服务管理器
net start mysql80

# 或使用MySQL命令行检查
mysql -u root -p
```

### 2. 启动Redis服务 (可选)
```bash
# Windows服务管理器
net start redis

# 或启动Redis服务器
redis-server
```

## 手动启动步骤

### 步骤1：启动后端服务

1. **打开命令行**：
   ```bash
   # 进入后端目录
   cd "E:\project\demo1\NexusDemo\nexus-backend"
   ```

2. **编译项目** (首次运行或代码修改后)：
   ```bash
   mvn clean compile -DskipTests
   ```

3. **运行后端服务**：
   ```bash
   mvn spring-boot:run
   ```
   - 服务将在端口 8083 启动
   - API访问地址：http://localhost:8083/api
   - Swagger文档：http://localhost:8083/api/swagger-ui.html

### 步骤2：启动前端服务

1. **打开新的命令行窗口**：
   ```bash
   # 进入前端目录
   cd "E:\project\demo1\NexusDemo\nexus-frontend"
   ```

2. **安装依赖** (首次运行)：
   ```bash
   npm install
   ```

3. **运行前端开发服务器**：
   ```bash
   npm run dev
   ```
   - 服务将在端口 5173 启动
   - 访问地址：http://localhost:5173

## 验证服务是否正常运行

### 后端服务检查
```bash
# 检查后端API是否响应
curl http://localhost:8083/api/swagger-ui.html

# 或使用浏览器访问：
# http://localhost:8083/api/swagger-ui.html
```

### 前端服务检查
- 浏览器访问：http://localhost:5173
- 应该能看到前端界面

## 手动停止服务

### 停止后端服务
1. 在后端命令行窗口中按 `Ctrl + C`
2. 等待进程完全停止

### 停止前端服务
1. 在前端命令行窗口中按 `Ctrl + C`
2. 等待进程完全停止

## 故障排除

### 端口冲突
如果端口被占用，可以修改配置文件：

**后端端口修改**：
1. 编辑 `nexus-backend/src/main/resources/application.yml`
2. 修改 `server.port` 的值
3. 重启后端服务

**前端端口修改**：
1. 前端端口由Vite自动分配，通常不需要修改
2. 如果必须修改，可以在 `package.json` 中修改 `dev` 脚本：
   ```json
   "scripts": {
     "dev": "vite --port 3000"
   }
   ```

### 数据库连接失败
1. 检查MySQL服务是否运行
2. 检查数据库连接配置：
   - 文件：`nexus-backend/src/main/resources/application.yml`
   - 确保 `spring.datasource.url`、`username`、`password` 正确

### 依赖安装失败
1. 使用国内镜像源：
   ```bash
   # Maven镜像
   # 编辑 ~/.m2/settings.xml

   # npm镜像
   npm config set registry https://registry.npmmirror.com
   ```

## 快速启动命令总结

```bash
# 窗口1 - 后端
cd nexus-backend && mvn spring-boot:run

# 窗口2 - 前端
cd nexus-frontend && npm run dev
```

## PowerShell脚本使用

如果批处理脚本无法运行，可以使用PowerShell脚本：

1. **右键点击** `start.ps1` 文件
2. 选择 **"使用PowerShell运行"**
3. 如果出现安全警告，输入 `Y` 确认

或从命令行运行：
```powershell
# 以管理员身份运行
powershell -ExecutionPolicy Bypass -File start.ps1
```

## 开发注意事项

1. **保持两个服务都运行** - 前端需要后端API
2. **修改代码后** - 后端需要重启，前端会自动热重载
3. **查看日志** - 后端日志在命令行窗口，前端错误在浏览器控制台

## 常见问题

### Q: 脚本双击没有反应
A: 可能是文件关联问题：
1. 尝试右键 -> "打开方式" -> "命令提示符"
2. 或使用PowerShell脚本 (`start.ps1`)
3. 或按照本文档手动启动

### Q: 提示"java不是内部或外部命令"
A: JDK未安装或未配置环境变量：
1. 安装JDK 21+
2. 设置JAVA_HOME环境变量
3. 将%JAVA_HOME%\bin添加到PATH

### Q: 前端无法访问后端API
A: 检查代理配置：
1. 确保后端服务正在运行 (端口8083)
2. 检查 `nexus-frontend/vite.config.js` 中的代理配置
3. 检查浏览器控制台的网络请求