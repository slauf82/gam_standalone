@echo off
setlocal
cd /d "%~dp0"

echo [GAM] Schritt 36 - PiperTTS Standardinstallation
echo.
echo Dieses Skript richtet Piper ein und laedt die Standardstimmen:
echo   Deutsch, Englisch, Franzoesisch
echo.
echo Wichtig:
echo   Es wird das komplette Windows-Piper-Release-ZIP geladen,
echo   nicht nur eine einzelne piper.exe. Piper braucht DLLs und Datenordner.
echo.
echo Weitere Stimmen werden spaeter automatisch im Hintergrund geladen,
echo sobald die UI-/Vorlesesprache gewechselt wird.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-piper-standard-voices.ps1"
if errorlevel 1 (
  echo.
  echo [FEHLER] PiperTTS konnte nicht vollstaendig installiert werden.
  echo Bitte die Fehlermeldung oben pruefen.
  pause
  exit /b 1
)

echo.
echo [GAM] Fertig. Test:
echo   CHECK_PIPER_TTS.bat
echo   http://localhost:8080/api/tts/status
pause
