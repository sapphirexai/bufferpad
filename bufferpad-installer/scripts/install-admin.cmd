@echo off
setlocal
set "SCRIPT_DIR=%~dp0"

net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Start-Process -FilePath '%ComSpec%' -Verb RunAs -ArgumentList '/d /c ""%~f0""'; exit 0 } catch { Write-Host ('Failed to open Administrator install window: ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }"
    if errorlevel 1 (
        echo.
        echo Install failed: Administrator window could not be opened. UAC may have been cancelled.
        pause
    )
    exit /b
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run-admin-wrapper.ps1" -ScriptPath "%SCRIPT_DIR%install.ps1" -Action Install
exit /b %errorlevel%
