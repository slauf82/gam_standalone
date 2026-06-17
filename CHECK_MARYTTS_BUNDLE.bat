@echo off
setlocal
cd /d "%~dp0"
echo GAM 2.0 - MaryTTS Diagnose
echo.
if exist "tts\marytts\bin\marytts-server.bat" (
  echo OK: tts\marytts\bin\marytts-server.bat gefunden
) else if exist "backend\tts\marytts\bin\marytts-server.bat" (
  echo OK: backend\tts\marytts\bin\marytts-server.bat gefunden
) else (
  echo Hinweis: Kein echtes MaryTTS-Bundle gefunden.
  echo Browser-TTS bleibt der Standard und funktioniert ohne MaryTTS.
  echo Optional: INSTALL_MARYTTS_BUNDLE.bat ausfuehren.
)
echo.
echo Backend-Status, falls Backend laeuft:
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Invoke-RestMethod 'http://localhost:8080/api/tts/status' | ConvertTo-Json -Depth 5 } catch { Write-Host 'Backend oder TTS-Status nicht erreichbar.' }"
pause
