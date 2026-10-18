@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\ensure-node.ps1"
if errorlevel 1 (
  echo.
  echo [FEHLER] Node.js/npm konnte nicht automatisch eingerichtet werden.
  pause
  exit /b 1
)
cd /d "%~dp0frontend"
if errorlevel 1 (
  echo [FEHLER] Frontend-Verzeichnis nicht erreichbar.
  pause
  exit /b 1
)
rem FIX46: npm-Paketcache ist optional, aber node_modules bleibt direkt im Frontend.
rem Keine Junctions (mklink /J) auf gemappten oder freigegebenen Laufwerken.
if not defined npm_config_cache set "npm_config_cache=%LOCALAPPDATA%\GAM-Standalone\npm-cache"
for /f "usebackq delims=" %%H in (`powershell -NoProfile -Command "(Get-FileHash -Algorithm SHA256 'package-lock.json').Hash.ToLower()"`) do set "GAM_LOCK_HASH=%%H"
if not defined GAM_LOCK_HASH (
  echo [FEHLER] package-lock.json fehlt oder ist nicht lesbar.
  pause
  exit /b 1
)
set "GAM_STORED_HASH="
if exist "node_modules\.gam-package-lock.sha256" set /p GAM_STORED_HASH=<"node_modules\.gam-package-lock.sha256"
if /I "%GAM_STORED_HASH%"=="%GAM_LOCK_HASH%" if exist "node_modules\vite\bin\vite.js" if exist "node_modules\typescript\lib\tsc.js" goto start_frontend

rem Alte FIX41-Junction entfernen, ohne das Cache-Ziel zu loeschen.
if exist "node_modules" (
  powershell -NoProfile -Command "$p=Get-Item -LiteralPath 'node_modules' -Force -ErrorAction SilentlyContinue; if($p -and ($p.Attributes -band [IO.FileAttributes]::ReparsePoint)){ exit 10 } else { exit 0 }" >nul 2>&1
  if errorlevel 10 (
    rmdir "node_modules"
    if exist "node_modules" (
      echo [FEHLER] Alte node_modules-Verknuepfung konnte nicht entfernt werden.
      pause
      exit /b 1
    )
  )
)
echo [GAM] Frontend-Abhaengigkeiten werden lokal eingerichtet bzw. aktualisiert...
call npm ci
if errorlevel 1 (
  echo [FEHLER] npm ci fehlgeschlagen.
  pause
  exit /b 1
)
if not exist "node_modules\vite\bin\vite.js" (
  echo [FEHLER] Vite fehlt trotz erfolgreichem npm ci.
  pause
  exit /b 1
)
if not exist "node_modules\typescript\lib\tsc.js" (
  echo [FEHLER] TypeScript fehlt trotz erfolgreichem npm ci.
  pause
  exit /b 1
)
>"node_modules\.gam-package-lock.sha256" echo %GAM_LOCK_HASH%
:start_frontend
call npm run dev
if errorlevel 1 echo [FEHLER] Frontend-Start fehlgeschlagen.
pause
