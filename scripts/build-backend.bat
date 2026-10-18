@echo off
cd /d %~dp0\..
set "GAM_ROOT=%CD%"
call mvnw.cmd clean package -DskipTests
