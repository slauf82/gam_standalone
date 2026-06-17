\
@echo off
setlocal
cd /d "%~dp0..\backend"

echo Removing cached failed MaryTTS dependency lookups...
rmdir /s /q "%USERPROFILE%\.m2\repository\com\twmacinta\fast-md5" 2>nul
rmdir /s /q "%USERPROFILE%\.m2\repository\gov\nist\math\Jampack" 2>nul
rmdir /s /q "%USERPROFILE%\.m2\repository\de\dfki\lt\jtok\jtok-core" 2>nul

echo.
echo Building with forced Maven dependency update...
call mvnw.cmd -U clean package

pause
