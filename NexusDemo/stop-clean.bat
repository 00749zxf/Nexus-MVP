@echo off
chcp 65001 >nul
echo ========================================
echo    Nexus Demo - Stop Script
echo ========================================
echo.

cd /d %~dp0

echo [Info] Stopping backend (port 8083)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8083" ^| findstr "LISTENING"') do (
    taskkill /PID %%a /F >nul 2>nul
    echo [OK] Backend stopped, PID: %%a
)

echo [Info] Stopping frontend (port 5173)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
    taskkill /PID %%a /F >nul 2>nul
    echo [OK] Frontend stopped, PID: %%a
)

echo.
echo ========================================
echo [Done] All services stopped!
echo ========================================
echo.

REM Verify ports are free
timeout /t 2 /nobreak >nul
netstat -ano | findstr ":8083" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo [Warning] Port 8083 still in use
) else (
    echo [OK] Port 8083 released
)

netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo [Warning] Port 5173 still in use
) else (
    echo [OK] Port 5173 released
)

pause