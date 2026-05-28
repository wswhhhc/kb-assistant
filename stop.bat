@echo off
chcp 65001 >nul

echo ========================================
echo  Stop Services (Frontend + FastAPI)
echo ========================================
echo.

echo Stopping FastAPI (port 8000)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8000"') do (
    taskkill /F /PID %%a 2>nul
)

echo Stopping Frontend (port 3000)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000"') do (
    taskkill /F /PID %%a 2>nul
)

echo.
echo Done! Spring Boot was not stopped (please stop via IDE).
pause
