@echo off
setlocal
cd /d "%~dp0..\backend"
set SPRING_PROFILES_ACTIVE=local
set MAVEN_OPTS=--add-opens=java.base/java.lang=ALL-UNNAMED %MAVEN_OPTS%
call mvnw.cmd -U clean spring-boot:run
pause
