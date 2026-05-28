@echo off
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do (
    if not "%%a"=="" (
        taskkill /F /PID %%a >nul 2>&1 && echo Released port 8080
    )
)
