@echo off
cd /d %~dp0

:check
netstat -ano | findstr ":9001" | findstr "LISTENING" >nul
if %errorlevel% == 0 (
    echo Port 9001 is in use, server is running...
) else (
    echo Port 9001 is not in use, starting server with prod profile...
    start "" javaw -jar "%~dp0opc-0.0.1.-SNAPSHOT.jar" --spring.profiles.active=prod
)

timeout /t 60
goto check