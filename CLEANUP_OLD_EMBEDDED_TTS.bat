@echo off
setlocal
cd /d "%~dp0"

echo [GAM Schritt 34c2] Entferne alte Embedded-MaryTTS-Reste, falls vorhanden...

if exist "backend\src\main\java\de\kopfzentrum\gam\tts\EmbeddedMaryTtsService.java" (
  del "backend\src\main\java\de\kopfzentrum\gam\tts\EmbeddedMaryTtsService.java"
  echo Entfernt: backend\src\main\java\de\kopfzentrum\gam\tts\EmbeddedMaryTtsService.java
) else (
  echo OK: EmbeddedMaryTtsService.java ist nicht vorhanden.
)

if exist "backend\target" (
  rmdir /s /q "backend\target"
  echo Entfernt: backend\target
) else (
  echo OK: backend\target ist nicht vorhanden.
)

echo.
echo Danach bitte im Backend ausfuehren:
echo   mvn clean compile
echo.
pause
