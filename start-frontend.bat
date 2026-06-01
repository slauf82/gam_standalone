@echo off
setlocal
cd /d "%~dp0\frontend"
where npm >nul 2>nul
if errorlevel 1 (
  echo Node.js/npm wurde nicht gefunden.
  pause
  exit /b 1
)
if not exist node_modules npm install
npm run dev
pause
