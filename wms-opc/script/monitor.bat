@echo off
:check
netstat -an | findstr :9001 >nul
if %errorlevel% == 0 (
    echo Port 9001 is in use, server is running...
) else (
    echo Port 9001 is not in use, starting server...
    start javaw -jar ..\target\opc-0.0.1.-SNAPSHOT.jar
)
timeout /t 60
goto check
rem for /f "tokens=5" %a in ('netstat -ano ^| findstr :9001') do taskkill /PID %a /F
