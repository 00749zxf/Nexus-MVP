# ========================================
#    Nexus电商系统Demo - PowerShell启动脚本
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Nexus电商系统Demo - 启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 函数：检查端口是否被占用
function Test-PortInUse {
    param([int]$Port, [string]$ServiceName)

    Write-Host "[检查] 检查$ServiceName端口 $Port..." -ForegroundColor Yellow
    $process = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

    if ($process) {
        Write-Host "[错误] 端口 $Port 已被占用 (PID: $($process.OwningProcess))，请先停止相关服务" -ForegroundColor Red
        Write-Host "        或修改配置文件中的端口号" -ForegroundColor Yellow
        Read-Host "按回车键退出"
        exit 1
    } else {
        Write-Host "[通过] 端口 $Port 可用" -ForegroundColor Green
    }
}

# 函数：检查服务健康状态
function Test-ServiceHealth {
    param([string]$Url, [int]$MaxAttempts = 30)

    $attempt = 1
    while ($attempt -le $MaxAttempts) {
        Write-Host "[等待] 尝试连接服务 (尝试 $attempt/$MaxAttempts)..." -ForegroundColor Yellow

        try {
            $response = Invoke-WebRequest -Uri $Url -Method Head -TimeoutSec 2 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Host "[通过] 服务已就绪！" -ForegroundColor Green
                return $true
            }
        } catch {
            # 忽略错误，继续重试
        }

        Write-Host "[等待] 服务尚未就绪，等待2秒..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2
        $attempt++
    }

    Write-Host "[错误] 服务启动超时，请检查日志" -ForegroundColor Red
    Read-Host "按回车键退出"
    exit 1
}

# 1. 检查环境依赖
Write-Host "[检查] 检查环境依赖..." -ForegroundColor Cyan

# 检查Node.js
if (-not (Get-Command "node" -ErrorAction SilentlyContinue)) {
    Write-Host "[错误] Node.js未安装或未添加到PATH" -ForegroundColor Red
    Write-Host "请先安装Node.js (https://nodejs.org/)" -ForegroundColor Yellow
    Read-Host "按回车键退出"
    exit 1
}
Write-Host "[通过] Node.js已安装" -ForegroundColor Green

# 检查Maven
if (-not (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
    Write-Host "[错误] Maven未安装或未添加到PATH" -ForegroundColor Red
    Write-Host "请先安装Maven (https://maven.apache.org/)" -ForegroundColor Yellow
    Read-Host "按回车键退出"
    exit 1
}
Write-Host "[通过] Maven已安装" -ForegroundColor Green

# 检查MySQL（可选）
if (-not (Get-Command "mysql" -ErrorAction SilentlyContinue)) {
    Write-Host "[警告] MySQL未安装或未添加到PATH" -ForegroundColor Yellow
    Write-Host "        数据库连接可能失败，请确保MySQL服务已启动" -ForegroundColor Yellow
} else {
    Write-Host "[通过] MySQL已安装" -ForegroundColor Green
}

# 检查Redis服务（可选）
Write-Host "[检查] 检查Redis服务..." -ForegroundColor Yellow
try {
    $null = & redis-cli ping 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[通过] Redis服务可用" -ForegroundColor Green
    } else {
        Write-Host "[警告] Redis服务可能未运行或未安装" -ForegroundColor Yellow
        Write-Host "        缓存功能可能不可用，但应用将继续启动" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[警告] Redis服务可能未运行或未安装" -ForegroundColor Yellow
    Write-Host "        缓存功能可能不可用，但应用将继续启动" -ForegroundColor Yellow
}

# 2. 检查端口占用
Write-Host "[信息] 检查端口占用情况..." -ForegroundColor Cyan
Test-PortInUse -Port 8083 -ServiceName "后端服务"
Test-PortInUse -Port 5173 -ServiceName "前端服务"

# 3. 检查项目依赖
Write-Host "[信息] 检查项目依赖..." -ForegroundColor Cyan

# 前端依赖
if (-not (Test-Path "nexus-frontend\node_modules\")) {
    Write-Host "[信息] 前端依赖未安装，正在安装..." -ForegroundColor Yellow
    Set-Location "nexus-frontend"

    try {
        & npm install
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[错误] 前端依赖安装失败" -ForegroundColor Red
            Read-Host "按回车键退出"
            exit 1
        }
    } catch {
        Write-Host "[错误] 前端依赖安装失败: $_" -ForegroundColor Red
        Read-Host "按回车键退出"
        exit 1
    }

    Set-Location ".."
    Write-Host "[通过] 前端依赖安装完成" -ForegroundColor Green
} else {
    Write-Host "[信息] 前端依赖已安装" -ForegroundColor Green
}

# 后端编译
if (-not (Test-Path "nexus-backend\target\")) {
    Write-Host "[信息] 后端项目未编译，正在编译..." -ForegroundColor Yellow
    Set-Location "nexus-backend"

    try {
        & mvn clean compile -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[错误] 后端项目编译失败" -ForegroundColor Red
            Read-Host "按回车键退出"
            exit 1
        }
    } catch {
        Write-Host "[错误] 后端项目编译失败: $_" -ForegroundColor Red
        Read-Host "按回车键退出"
        exit 1
    }

    Set-Location ".."
    Write-Host "[通过] 后端项目编译完成" -ForegroundColor Green
} else {
    Write-Host "[信息] 后端项目已编译" -ForegroundColor Green
}

Write-Host ""

# 4. 启动后端服务
Write-Host "[信息] 启动后端服务 (端口: 8083)..." -ForegroundColor Cyan
$backendJob = Start-Process -FilePath "cmd.exe" -ArgumentList "/k `"cd /d `"$PWD\nexus-backend`" && mvn spring-boot:run`"" -WindowStyle Normal -PassThru
Write-Host "[信息] 后端服务已启动，PID: $($backendJob.Id)" -ForegroundColor Green

Write-Host ""
Write-Host "[信息] 等待后端服务初始化..." -ForegroundColor Cyan

# 等待后端服务启动
Test-ServiceHealth -Url "http://localhost:8083/api/swagger-ui.html"

# 5. 启动前端服务
Write-Host "[信息] 启动前端服务 (端口: 5173)..." -ForegroundColor Cyan
$frontendJob = Start-Process -FilePath "cmd.exe" -ArgumentList "/k `"cd /d `"$PWD\nexus-frontend`" && npm run dev`"" -WindowStyle Normal -PassThru
Write-Host "[信息] 前端服务已启动，PID: $($frontendJob.Id)" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[成功] 服务启动完成！" -ForegroundColor Green
Write-Host ""
Write-Host "访问地址：" -ForegroundColor White
Write-Host "前端: http://localhost:5173" -ForegroundColor Yellow
Write-Host "后端API: http://localhost:8083/api" -ForegroundColor Yellow
Write-Host "Swagger文档: http://localhost:8083/api/swagger-ui.html" -ForegroundColor Yellow
Write-Host ""
Write-Host "注意：" -ForegroundColor White
Write-Host "1. 请确保MySQL和Redis服务已启动" -ForegroundColor Yellow
Write-Host "2. 关闭本窗口不会停止服务，服务会继续在后台运行" -ForegroundColor Yellow
Write-Host "3. 使用 stop.ps1 或 stop.bat 停止所有服务" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 保存进程ID到文件，供停止脚本使用
@{
    BackendPid = $backendJob.Id
    FrontendPid = $frontendJob.Id
    BackendPort = 8083
    FrontendPort = 5173
} | ConvertTo-Json | Out-File -FilePath ".claude\processes.json" -Force

Write-Host "进程信息已保存到 .claude\processes.json" -ForegroundColor Gray
Write-Host "按回车键关闭此窗口..." -ForegroundColor Gray
Read-Host