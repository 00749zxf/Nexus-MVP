#!/bin/bash

# ========================================
#    Nexus电商系统Demo - Shell停止脚本
#    适用于Git Bash、WSL或Linux/macOS
# ========================================

echo "========================================"
echo "   Nexus电商系统Demo - 停止脚本"
echo "========================================"
echo ""

# 函数：停止指定端口的服务
stop_port_service() {
    local port=$1
    local service_name=$2
    local pid=""

    echo "[信息] 停止${service_name} (端口: ${port})..."

    # 查找占用端口的进程
    if command -v netstat &> /dev/null; then
        # Windows Git Bash或Linux netstat
        pid=$(netstat -ano 2>/dev/null | grep ":${port}.*LISTENING" | awk '{print $5}' | head -1)
    elif command -v lsof &> /dev/null; then
        # macOS或Linux lsof
        pid=$(lsof -t -i :${port} 2>/dev/null | head -1)
    fi

    if [ -z "$pid" ]; then
        echo "[信息] ${service_name}未运行或端口未被占用"
        return 0
    fi

    echo "[信息] 找到进程PID: ${pid}，正在停止..."

    # 尝试优雅停止
    if kill $pid 2>/dev/null; then
        echo "[信息] 已发送停止信号，等待进程退出..."
    else
        echo "[警告] 优雅停止失败，尝试强制停止..."
        if kill -9 $pid 2>/dev/null; then
            echo "[信息] 已发送强制停止信号"
        else
            echo "[警告] 强制停止失败"
            return 1
        fi
    fi

    # 检查进程是否已停止，最多等待5秒
    local max_checks=5
    local check_count=0
    local process_stopped=false

    while [ $check_count -lt $max_checks ]; do
        sleep 1
        if ! kill -0 $pid 2>/dev/null; then
            process_stopped=true
            break
        fi
        check_count=$((check_count + 1))
        echo "[等待] 等待进程退出 (${check_count}/${max_checks})..."
    done

    if [ "$process_stopped" = false ]; then
        echo "[警告] 进程 ${pid} 仍在运行，尝试其他方法..."
        # 根据服务名称尝试停止相关进程
        if [ "$service_name" = "后端服务" ]; then
            pkill -f "spring-boot:run" 2>/dev/null || true
        elif [ "$service_name" = "前端服务" ]; then
            pkill -f "npm run dev" 2>/dev/null || true
        fi
        sleep 2
    fi

    # 最终确认端口是否释放
    sleep 1
    local port_still_in_use=false

    if command -v netstat &> /dev/null; then
        if netstat -ano 2>/dev/null | grep -q ":${port}.*LISTENING"; then
            port_still_in_use=true
        fi
    elif command -v lsof &> /dev/null; then
        if lsof -i :${port} &>/dev/null; then
            port_still_in_use=true
        fi
    fi

    if [ "$port_still_in_use" = true ]; then
        echo "[警告] 端口 ${port} 仍被占用，可能需要手动检查"
        return 1
    else
        echo "[成功] ${service_name}已停止，端口 ${port} 已释放"
        return 0
    fi
}

# 从进程文件读取进程ID（如果存在）
if [ -f ".claude/processes.sh" ]; then
    echo "[信息] 从进程文件读取进程信息..."
    source ".claude/processes.sh" 2>/dev/null || true

    if [ -n "$BACKEND_PID" ]; then
        echo "[信息] 停止后端服务 (PID: ${BACKEND_PID})..."
        kill -9 "$BACKEND_PID" 2>/dev/null || true
        echo "[信息] 后端服务已停止"
    fi

    if [ -n "$FRONTEND_PID" ]; then
        echo "[信息] 停止前端服务 (PID: ${FRONTEND_PID})..."
        kill -9 "$FRONTEND_PID" 2>/dev/null || true
        echo "[信息] 前端服务已停止"
    fi

    # 删除进程文件
    rm -f ".claude/processes.sh" 2>/dev/null || true
    echo "[信息] 已删除进程文件"
fi

# 通过端口检测停止服务
echo ""
echo "[信息] 通过端口检测停止服务..."

backend_stopped=true
frontend_stopped=true

if ! stop_port_service 8083 "后端服务"; then
    backend_stopped=false
fi

if ! stop_port_service 5173 "前端服务"; then
    frontend_stopped=false
fi

echo ""
echo "========================================"

if $backend_stopped && $frontend_stopped; then
    echo "[成功] 所有服务已停止！"
else
    if ! $backend_stopped; then
        echo "[警告] 后端服务可能未完全停止"
    fi
    if ! $frontend_stopped; then
        echo "[警告] 前端服务可能未完全停止"
    fi
    echo "[信息] 如果服务未完全停止，请手动检查进程"
fi

echo "========================================"
echo ""

echo "按回车键关闭此窗口..."
read