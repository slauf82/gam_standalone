@echo off
setlocal EnableExtensions
set "BASEDIR=%~dp0"

where mvn >nul 2>nul
if %errorlevel%==0 (
  mvn -f "%BASEDIR%backend\pom.xml" %*
  exit /b %errorlevel%
)

set "MVN_BASE=%BASEDIR%.mvn"
set "MVN_DIR=%MVN_BASE%\apache-maven-3.9.11"
set "MVN_ZIP=%MVN_BASE%\apache-maven-3.9.11-bin.zip"
set "MVN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip"

if not exist "%MVN_DIR%\bin\mvn.cmd" (
  echo Maven wurde nicht gefunden. Versuche Maven 3.9.11 lokal in .mvn\ zu laden...

  if not exist "%MVN_BASE%" (
    mkdir "%MVN_BASE%"
    if errorlevel 1 (
      echo [FEHLER] Das Maven-Zielverzeichnis konnte nicht erstellt werden:
      echo          %MVN_BASE%
      exit /b 1
    )
  )

  if exist "%MVN_ZIP%" del /q "%MVN_ZIP%" >nul 2>nul

  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop';" ^
    "Write-Host '[GAM] Lade Maven 3.9.11 herunter...';" ^
    "Invoke-WebRequest -UseBasicParsing -Uri '%MVN_URL%' -OutFile '%MVN_ZIP%';" ^
    "if (-not (Test-Path -LiteralPath '%MVN_ZIP%')) { throw 'Maven-Archiv wurde nicht angelegt.' };" ^
    "Write-Host '[GAM] Entpacke Maven...';" ^
    "Expand-Archive -LiteralPath '%MVN_ZIP%' -DestinationPath '%MVN_BASE%' -Force"
  if errorlevel 1 (
    echo [FEHLER] Maven konnte nicht heruntergeladen oder entpackt werden.
    echo Bitte Internetzugriff pruefen oder Apache Maven 3.9.11 manuell nach .mvn entpacken.
    exit /b 1
  )

  if not exist "%MVN_DIR%\bin\mvn.cmd" (
    echo [FEHLER] Maven wurde entpackt, aber mvn.cmd wurde nicht gefunden:
    echo          %MVN_DIR%\bin\mvn.cmd
    exit /b 1
  )

  del /q "%MVN_ZIP%" >nul 2>nul
  echo [GAM] Maven 3.9.11 wurde lokal eingerichtet.
)

call "%MVN_DIR%\bin\mvn.cmd" -f "%BASEDIR%backend\pom.xml" %*
exit /b %errorlevel%
