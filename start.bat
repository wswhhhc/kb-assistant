@echo off
chcp 65001 >nul

echo ========================================
echo  KB Assistant - Start Services
echo ========================================
echo.

set ROOT=D:\AI Demo\kb-assistant

:: ========== 1. Start Frontend (Vue 3) ==========
echo [1/2] Starting Frontend (Vue 3) ...
start "kb-frontend" /D "%ROOT%\frontend" cmd /c "npm run dev"

:: ========== 2. Start FastAPI ==========
echo [2/2] Starting FastAPI AI Service ...
start "kb-fastapi" /D "%ROOT%\backend-python" cmd /c "python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000"

echo.
echo Services started!
echo.
echo Service URLs:
echo   Frontend:    http://localhost:3000
echo   FastAPI:     http://localhost:8000
echo   API Docs:    http://localhost:8000/docs
echo.
echo Spring Boot:  start manually via IDE or "mvn spring-boot:run"
echo.
pause
