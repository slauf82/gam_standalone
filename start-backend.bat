@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if not exist logs mkdir logs
if exist .env (
  for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" if not "%%A:~0,1"=="#" set "%%A=%%B"
  )
)
set SPRING_PROFILES_ACTIVE=local

REM Preview 2: Java 21 automatisch pruefen und bei Bedarf lokal einrichten.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\ensure-java.ps1"
if errorlevel 1 (
  echo.
  echo [WARNUNG] Die automatische Java-Erkennung war nicht erfolgreich.
  echo [GAM] Vorhandene Java-/Javac-Installation wird direkt geprueft...
  java -version >nul 2>&1
  if errorlevel 1 (
    echo [FEHLER] Java ist weder automatisch erkannt noch ueber PATH erreichbar.
    echo Fuer einen automatischen Download ist Internetzugriff erforderlich.
    pause
    exit /b 1
  )
  javac -version >nul 2>&1
  if errorlevel 1 (
    echo [FEHLER] Java ist vorhanden, aber kein JDK mit javac erreichbar.
    echo Bitte JAVA_HOME auf Ihr JDK 21 setzen oder JDK 21 in PATH aufnehmen.
    pause
    exit /b 1
  )
  echo [GAM] Vorhandenes JDK aus PATH wird verwendet.
)
if exist "%~dp0runtime\java-home.txt" (
  set /p "JAVA_HOME="<"%~dp0runtime\java-home.txt"
  if exist "!JAVA_HOME!\bin\java.exe" if exist "!JAVA_HOME!\bin\javac.exe" (
    set "PATH=!JAVA_HOME!\bin;!PATH!"
    echo [GAM] Verwendetes JDK: !JAVA_HOME!
  )
) else if exist "%~dp0runtime\java\bin\java.exe" (
  set "JAVA_HOME=%~dp0runtime\java"
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)

REM Schritt 36h: MariaDB/MySQL sicher erkennen und Demo-Datenbank optional vorbereiten.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\ensure-gam-database.ps1"
if errorlevel 1 (
  echo.
  echo [WARNUNG] Datenbank-Setup meldete einen Fehler. Backend-Start wird versucht.
  echo Bitte logs und .env pruefen, falls der Backend-Start fehlschlaegt.
)

REM Schritt 36b: Piper-Basis automatisch vorbereiten, damit start-backend.bat + start-frontend.bat genuegen.
set PIPER_NEEDS_INSTALL=0
if not exist "tts\piper\piper.exe" set PIPER_NEEDS_INSTALL=1
if not exist "tts\piper\voices\de_DE-thorsten-medium.onnx" set PIPER_NEEDS_INSTALL=1
if not exist "tts\piper\voices\en_US-lessac-medium.onnx" set PIPER_NEEDS_INSTALL=1
if not exist "tts\piper\voices\fr_FR-siwis-medium.onnx" set PIPER_NEEDS_INSTALL=1
if "%PIPER_NEEDS_INSTALL%"=="1" (
  echo.
  echo [GAM] PiperTTS fehlt oder ist unvollstaendig. Installation wird vor Backend-Start ausgefuehrt...
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-piper-standard-voices.ps1"
  if errorlevel 1 (
    echo.
    echo [WARNUNG] PiperTTS konnte nicht automatisch installiert werden.
    echo Backend startet trotzdem; Browser-TTS bleibt als Fallback verfuegbar.
    echo Details siehe Ausgabe oben.
  )
) else (
  echo [GAM] PiperTTS Basis vorhanden.
)
call mvnw.cmd clean package
if errorlevel 1 (
  echo.
  echo Build fehlgeschlagen. Backend wird nicht gestartet.
  pause
  exit /b 1
)
call mvnw.cmd spring-boot:run
pause
