@echo off
chcp 65001 >nul

echo ========================================
echo  KB Assistant - Start All Services
echo ========================================
echo.

:: Set Java environment
set JAVA_HOME=D:\java\jdk
set PATH=%JAVA_HOME%\bin;%PATH%

set ROOT=D:\AI Demo\kb-assistant

:: ========== 1. Start Frontend (Vue 3) ==========
echo [1/3] Starting Frontend (Vue 3) ...
start "kb-frontend" cmd /c "cd /d %ROOT%\frontend && npm run dev"

:: ========== 2. Start FastAPI ==========
echo [2/3] Starting FastAPI AI Service ...
start "kb-fastapi" cmd /c "cd /d %ROOT%\backend-python && python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000"

:: ========== 3. Start Spring Boot ==========
echo [3/3] Starting Spring Boot Service ...
start "kb-springboot" cmd /c "cd /d %ROOT%\backend-java && mvn spring-boot:run -Dspring-boot.run.profiles=dev"

echo.
echo All services started!
echo.
echo Service URLs:
echo   Frontend:    http://localhost:3000
echo   FastAPI:     http://localhost:8000
echo   Spring Boot: http://localhost:8080
echo   API Docs:    http://localhost:8000/docs
echo.
echo Use stop.bat to stop all services
pause
