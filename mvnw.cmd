@echo off
setlocal
set BASEDIR=%~dp0
where mvn >nul 2>nul
if %errorlevel%==0 (
  mvn -f "%BASEDIR%backend\pom.xml" %*
  exit /b %errorlevel%
)
set MVN_DIR=%BASEDIR%.mvn\apache-maven-3.9.11
set MVN_ZIP=%BASEDIR%.mvn\apache-maven-3.9.11-bin.zip
if not exist "%MVN_DIR%\bin\mvn.cmd" (
  echo Maven wurde nicht gefunden. Lade Maven 3.9.11 lokal nach .mvn\ ...
  powershell -ExecutionPolicy Bypass -NoProfile -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -OutFile '%MVN_ZIP%'; Expand-Archive -Path '%MVN_ZIP%' -DestinationPath '%BASEDIR%.mvn' -Force"
  if errorlevel 1 (
    echo Bitte Maven installieren oder apache-maven-3.9.11 nach .mvn entpacken.
    exit /b 1
  )
)
"%MVN_DIR%\bin\mvn.cmd" -f "%BASEDIR%backend\pom.xml" %*
exit /b %errorlevel%
