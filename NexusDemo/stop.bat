@echo off
setlocal enabledelayedexpansion
echo ========================================
echo    Nexus电商系统Demo - 停止脚本
echo ========================================
echo.

setlocal enabledelayedexpansion

echo [信息] 正在停止服务...

REM 函数：杀死指定端口的进程
call :kill_port_process 8083 "后端服务"
call :kill_port_process 5173 "前端服务"

echo.
echo ========================================
echo [成功] 所有服务已停止！
echo ========================================
echo.
pause
exit /b

:kill_port_process
setlocal enabledelayedexpansion
set port=%1
set service_name=%2

echo [信息] 停止%service_name% (端口: %port%)...

REM 方法1: 使用netstat查找端口进程
set pid=
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port%" ^| findstr "LISTENING"') do (
    set pid=%%a
)

if not defined pid (
    echo [信息] %service_name%未运行或端口未被占用
    goto :check_stopped
)

echo [信息] 找到进程PID: !pid!，正在停止...

REM 尝试优雅停止
taskkill /PID !pid! >nul 2>nul
if %errorlevel% equ 0 (
    echo [信息] 已发送停止信号，等待进程退出...
) else (
    echo [警告] 优雅停止失败，尝试强制停止...
    taskkill /PID !pid! /F >nul 2>nul
)

REM 检查进程是否已停止，最多等待5秒
set max_checks=5
set check_count=0
:check_process
timeout /t 1 /nobreak >nul
tasklist /FI "PID eq !pid!" 2>nul | findstr "!pid!" >nul
if %errorlevel% equ 0 (
    set /a check_count+=1
    if !check_count! geq !max_checks! (
        echo [警告] 进程 !pid! 仍在运行，尝试其他方法...
        REM 尝试通过窗口标题停止
        if "%service_name%"=="后端服务" (
            taskkill /FI "WINDOWTITLE eq Nexus Backend" /F >nul 2>nul
        ) else if "%service_name%"=="前端服务" (
            taskkill /FI "WINDOWTITLE eq Nexus Frontend" /F >nul 2>nul
        )
        timeout /t 2 /nobreak >nul
    ) else (
        echo [等待] 等待进程退出 (%check_count%/%max_checks%)...
        goto check_process
    )
)

:check_stopped
REM 最终确认端口是否释放
timeout /t 1 /nobreak >nul
set port_still_in_use=false
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port%" ^| findstr "LISTENING"') do (
    set port_still_in_use=true
)
if "!port_still_in_use!"=="true" (
    echo [警告] 端口 %port% 仍被占用，可能需要手动检查
) else (
    echo [成功] %service_name%已停止，端口 %port% 已释放
)
endlocal
goto :eof