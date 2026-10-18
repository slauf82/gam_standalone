@echo off
setlocal
set MODE=%~1
if "%MODE%"=="" set MODE=FULL
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0gam-auto-scan.ps1" -Mode %MODE%
exit /b %ERRORLEVEL%
