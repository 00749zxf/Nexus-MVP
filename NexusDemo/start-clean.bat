@echo off
chcp 65001 >nul
echo ========================================
echo    Nexus Demo - Start Script
echo ========================================
echo.

cd /d %~dp0

echo [Check] Port 8083...
netstat -ano | findstr ":8083" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo [Error] Port 8083 in use
    pause
    exit /b 1
)
echo [OK] Port 8083 available

echo [Check] Port 5173...
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if %errorlevel% equ 0 (
    echo [Error] Port 5173 in use
    pause
    exit /b 1
)
echo [OK] Port 5173 available

echo [Info] Starting backend...
start "Nexus Backend" cmd /k "cd /d %~dp0nexus-backend && mvn spring-boot:run"

echo [Info] Waiting for backend (30s)...
timeout /t 30 /nobreak >nul

echo [Info] Starting frontend...
start "Nexus Frontend" cmd /k "cd /d %~dp0nexus-frontend && npm run dev"

echo.
echo ========================================
echo [Done] Services started!
echo.
echo Frontend: http://localhost:5173
echo Backend:  http://localhost:8083/api
echo Swagger:  http://localhost:8083/api/swagger-ui.html
echo ========================================
echo.
pause