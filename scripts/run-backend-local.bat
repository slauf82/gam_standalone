@echo off
setlocal
cd /d %~dp0\..
set "GAM_ROOT=%CD%"
set SPRING_PROFILES_ACTIVE=local
call mvnw.cmd spring-boot:run
