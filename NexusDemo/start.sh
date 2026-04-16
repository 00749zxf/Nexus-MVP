#!/bin/bash

# ========================================
#    Nexus电商系统Demo - Shell启动脚本
#    适用于Git Bash、WSL或Linux/macOS
# ========================================

echo "========================================"
echo "   Nexus电商系统Demo - 启动脚本"
echo "========================================"
echo ""

# 函数：检查端口是否被占用
check_port() {
    local port=$1
    local service_name=$2

    echo "[检查] 检查${service_name}端口 ${port}..."

    if command -v netstat &> /dev/null; then
        # Windows Git Bash或Linux netstat
        if netstat -ano 2>/dev/null | grep -q ":${port}.*LISTENING"; then
            local pid=$(netstat -ano 2>/dev/null | grep ":${port}.*LISTENING" | awk '{print $5}' | head -1)
            echo "[错误] 端口 ${port} 已被占用 (PID: ${pid})，请先停止相关服务"
            echo "        或修改配置文件中的端口号"
            read -p "按回车键退出"
            exit 1
        fi
    elif command -v lsof &> /dev/null; then
        # macOS或Linux lsof
        if lsof -i :${port} &> /dev/null; then
            local pid=$(lsof -t -i :${port} | head -1)
            echo "[错误] 端口 ${port} 已被占用 (PID: ${pid})，请先停止相关服务"
            echo "        或修改配置文件中的端口号"
            read -p "按回车键退出"
            exit 1
        fi
    else
        echo "[警告] 无法检查端口占用，跳过检查" >&2
    fi

    echo "[通过] 端口 ${port} 可用"
}

# 函数：检查服务健康状态
check_service_health() {
    local url=$1
    local max_attempts=${2:-30}
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        echo "[等待] 尝试连接服务 (尝试 ${attempt}/${max_attempts})..."

        if command -v curl &> /dev/null; then
            if curl -s -f -o /dev/null --max-time 2 "$url"; then
                echo "[通过] 服务已就绪！"
                return 0
            fi
        elif command -v wget &> /dev/null; then
            if wget -q --timeout=2 --spider "$url"; then
                echo "[通过] 服务已就绪！"
                return 0
            fi
        else
            echo "[警告] 没有curl或wget，跳过健康检查" >&2
            return 0
        fi

        echo "[等待] 服务尚未就绪，等待2秒..."
        sleep 2
        attempt=$((attempt + 1))
    done

    echo "[错误] 服务启动超时，请检查日志"
    read -p "按回车键退出"
    exit 1
}

# 1. 检查环境依赖
echo "[检查] 检查环境依赖..."

# 检查Node.js
if ! command -v node &> /dev/null; then
    echo "[错误] Node.js未安装或未添加到PATH"
    echo "请先安装Node.js (https://nodejs.org/)"
    read -p "按回车键退出"
    exit 1
fi
echo "[通过] Node.js已安装"

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "[错误] Maven未安装或未添加到PATH"
    echo "请先安装Maven (https://maven.apache.org/)"
    read -p "按回车键退出"
    exit 1
fi
echo "[通过] Maven已安装"

# 检查MySQL（可选）
if ! command -v mysql &> /dev/null; then
    echo "[警告] MySQL未安装或未添加到PATH"
    echo "        数据库连接可能失败，请确保MySQL服务已启动"
else
    echo "[通过] MySQL已安装"
fi

# 检查Redis服务（可选）
echo "[检查] 检查Redis服务..."
if command -v redis-cli &> /dev/null; then
    if redis-cli ping &> /dev/null; then
        echo "[通过] Redis服务可用"
    else
        echo "[警告] Redis服务可能未运行或未安装"
        echo "        缓存功能可能不可用，但应用将继续启动"
    fi
else
    echo "[警告] Redis服务可能未运行或未安装"
    echo "        缓存功能可能不可用，但应用将继续启动"
fi

# 2. 检查端口占用
echo "[信息] 检查端口占用情况..."
check_port 8083 "后端服务"
check_port 5173 "前端服务"

# 3. 检查项目依赖
echo "[信息] 检查项目依赖..."

# 前端依赖
if [ ! -d "nexus-frontend/node_modules" ]; then
    echo "[信息] 前端依赖未安装，正在安装..."
    cd nexus-frontend

    if ! npm install; then
        echo "[错误] 前端依赖安装失败"
        cd ..
        read -p "按回车键退出"
        exit 1
    fi

    cd ..
    echo "[通过] 前端依赖安装完成"
else
    echo "[信息] 前端依赖已安装"
fi

# 后端编译
if [ ! -d "nexus-backend/target" ]; then
    echo "[信息] 后端项目未编译，正在编译..."
    cd nexus-backend

    if ! mvn clean compile -DskipTests; then
        echo "[错误] 后端项目编译失败"
        cd ..
        read -p "按回车键退出"
        exit 1
    fi

    cd ..
    echo "[通过] 后端项目编译完成"
else
    echo "[信息] 后端项目已编译"
fi

echo ""

# 4. 启动后端服务
echo "[信息] 启动后端服务 (端口: 8083)..."
cd nexus-backend
mvn spring-boot:run &
BACKEND_PID=$!
cd ..
echo "[信息] 后端服务已启动，PID: ${BACKEND_PID}"

echo ""
echo "[信息] 等待后端服务初始化..."

# 等待后端服务启动
check_service_health "http://localhost:8083/api/swagger-ui.html"

# 5. 启动前端服务
echo "[信息] 启动前端服务 (端口: 5173)..."
cd nexus-frontend
npm run dev &
FRONTEND_PID=$!
cd ..
echo "[信息] 前端服务已启动，PID: ${FRONTEND_PID}"

# 保存进程ID到文件
mkdir -p .claude
cat > .claude/processes.sh << EOF
#!/bin/bash
# 进程信息文件
BACKEND_PID=${BACKEND_PID}
FRONTEND_PID=${FRONTEND_PID}
EOF
chmod +x .claude/processes.sh

echo ""
echo "========================================"
echo "[成功] 服务启动完成！"
echo ""
echo "访问地址："
echo "前端: http://localhost:5173"
echo "后端API: http://localhost:8083/api"
echo "Swagger文档: http://localhost:8083/api/swagger-ui.html"
echo ""
echo "注意："
echo "1. 请确保MySQL和Redis服务已启动"
echo "2. 按 Ctrl+C 可以停止脚本，但服务会继续在后台运行"
echo "3. 使用 stop.sh 停止所有服务"
echo "========================================"
echo ""

echo "进程信息已保存到 .claude/processes.sh"
echo "按回车键关闭此窗口..."
read

# 前台运行，按Ctrl+C可以停止
wait