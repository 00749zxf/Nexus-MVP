# ========================================
#    Nexus电商系统Demo - PowerShell停止脚本
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Nexus电商系统Demo - 停止脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 函数：停止指定端口的服务
function Stop-PortService {
    param([int]$Port, [string]$ServiceName)

    Write-Host "[信息] 停止$ServiceName (端口: $Port)..." -ForegroundColor Yellow

    # 查找占用端口的进程
    $process = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

    if (-not $process) {
        Write-Host "[信息] $ServiceName未运行或端口未被占用" -ForegroundColor Gray
        return $true
    }

    $pid = $process.OwningProcess
    Write-Host "[信息] 找到进程PID: $pid，正在停止..." -ForegroundColor Yellow

    # 尝试优雅停止
    try {
        Stop-Process -Id $pid -ErrorAction Stop
        Write-Host "[信息] 已发送停止信号，等待进程退出..." -ForegroundColor Yellow
    } catch {
        Write-Host "[警告] 优雅停止失败，尝试强制停止..." -ForegroundColor Yellow
        try {
            Stop-Process -Id $pid -Force -ErrorAction Stop
        } catch {
            Write-Host "[警告] 强制停止失败: $_" -ForegroundColor Red
            return $false
        }
    }

    # 检查进程是否已停止，最多等待5秒
    $maxChecks = 5
    $checkCount = 0
    $processStopped = $false

    while ($checkCount -lt $maxChecks) {
        Start-Sleep -Seconds 1
        $stillRunning = Get-Process -Id $pid -ErrorAction SilentlyContinue

        if (-not $stillRunning) {
            $processStopped = $true
            break
        }

        $checkCount++
        Write-Host "[等待] 等待进程退出 ($checkCount/$maxChecks)..." -ForegroundColor Yellow
    }

    if (-not $processStopped) {
        Write-Host "[警告] 进程 $pid 仍在运行，尝试通过窗口标题停止..." -ForegroundColor Yellow

        if ($ServiceName -eq "后端服务") {
            Stop-Process -Name "cmd" -ErrorAction SilentlyContinue | Where-Object {
                (Get-Process -Id $_.Id).MainWindowTitle -like "*Nexus Backend*"
            } | Stop-Process -Force -ErrorAction SilentlyContinue
        } elseif ($ServiceName -eq "前端服务") {
            Stop-Process -Name "cmd" -ErrorAction SilentlyContinue | Where-Object {
                (Get-Process -Id $_.Id).MainWindowTitle -like "*Nexus Frontend*"
            } | Stop-Process -Force -ErrorAction SilentlyContinue
        }
    }

    # 最终确认端口是否释放
    Start-Sleep -Seconds 1
    $portStillInUse = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

    if ($portStillInUse) {
        Write-Host "[警告] 端口 $Port 仍被占用，可能需要手动检查" -ForegroundColor Red
        return $false
    } else {
        Write-Host "[成功] $ServiceName已停止，端口 $Port 已释放" -ForegroundColor Green
        return $true
    }
}

# 从进程文件读取进程ID（如果存在）
$processFile = ".claude\processes.json"
if (Test-Path $processFile) {
    Write-Host "[信息] 从进程文件读取进程信息..." -ForegroundColor Yellow
    try {
        $processInfo = Get-Content $processFile -Raw | ConvertFrom-Json

        Write-Host "[信息] 找到保存的进程信息：" -ForegroundColor Yellow
        Write-Host "      后端PID: $($processInfo.BackendPid), 端口: $($processInfo.BackendPort)" -ForegroundColor Gray
        Write-Host "      前端PID: $($processInfo.FrontendPid), 端口: $($processInfo.FrontendPort)" -ForegroundColor Gray

        # 尝试通过PID停止进程
        $processesStopped = @()

        if ($processInfo.BackendPid) {
            try {
                Write-Host "[信息] 停止后端服务 (PID: $($processInfo.BackendPid))..." -ForegroundColor Yellow
                Stop-Process -Id $processInfo.BackendPid -Force -ErrorAction SilentlyContinue
                $processesStopped += "后端服务"
            } catch {
                Write-Host "[信息] 无法通过PID停止后端服务，尝试端口检测..." -ForegroundColor Yellow
            }
        }

        if ($processInfo.FrontendPid) {
            try {
                Write-Host "[信息] 停止前端服务 (PID: $($processInfo.FrontendPid))..." -ForegroundColor Yellow
                Stop-Process -Id $processInfo.FrontendPid -Force -ErrorAction SilentlyContinue
                $processesStopped += "前端服务"
            } catch {
                Write-Host "[信息] 无法通过PID停止前端服务，尝试端口检测..." -ForegroundColor Yellow
            }
        }

        if ($processesStopped.Count -gt 0) {
            Write-Host "[信息] 通过PID停止了以下服务: $($processesStopped -join ', ')" -ForegroundColor Green
        }

        # 删除进程文件
        Remove-Item $processFile -Force -ErrorAction SilentlyContinue
        Write-Host "[信息] 已删除进程文件" -ForegroundColor Gray

    } catch {
        Write-Host "[警告] 读取进程文件失败: $_" -ForegroundColor Yellow
        Write-Host "[信息] 将使用端口检测方法..." -ForegroundColor Yellow
    }
}

# 通过端口检测停止服务
Write-Host ""
Write-Host "[信息] 通过端口检测停止服务..." -ForegroundColor Cyan

$backendStopped = Stop-PortService -Port 8083 -ServiceName "后端服务"
$frontendStopped = Stop-PortService -Port 5173 -ServiceName "前端服务"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

if ($backendStopped -and $frontendStopped) {
    Write-Host "[成功] 所有服务已停止！" -ForegroundColor Green
} else {
    if (-not $backendStopped) {
        Write-Host "[警告] 后端服务可能未完全停止" -ForegroundColor Red
    }
    if (-not $frontendStopped) {
        Write-Host "[警告] 前端服务可能未完全停止" -ForegroundColor Red
    }
    Write-Host "[信息] 如果服务未完全停止，请手动检查任务管理器" -ForegroundColor Yellow
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "按回车键关闭此窗口..." -ForegroundColor Gray
Read-Host