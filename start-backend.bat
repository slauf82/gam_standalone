@echo off
setlocal
cd /d "%~dp0"
if not exist logs mkdir logs
if exist .env (
  for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" if not "%%A:~0,1"=="#" set "%%A=%%B"
  )
)
set SPRING_PROFILES_ACTIVE=local
call mvnw.cmd spring-boot:run
pause
