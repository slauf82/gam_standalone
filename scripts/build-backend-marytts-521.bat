@echo off
cd /d "%~dp0..\backend"
echo Building GAM backend with MaryTTS 5.2.1 Maven Central dependencies...
call mvnw.cmd -U clean package
pause
