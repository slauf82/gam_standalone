@echo off
setlocal
cd /d "%~dp0"

echo GAM 2.0 - MaryTTS Bundle installieren

echo.
echo Ziel: %CD%\tts\marytts

echo Download: MaryTTS 5.2 Runtime von SourceForge

echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$root=(Get-Location).Path;" ^
  "$target=Join-Path $root 'tts\marytts';" ^
  "$tmp=Join-Path $env:TEMP 'gam-marytts-install';" ^
  "$zip=Join-Path $tmp 'marytts-5.2.zip';" ^
  "Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue;" ^
  "New-Item -ItemType Directory -Path $tmp | Out-Null;" ^
  "New-Item -ItemType Directory -Path (Split-Path $target -Parent) -Force | Out-Null;" ^
  "$url='https://sourceforge.net/projects/marytts.mirror/files/v5.2/marytts-5.2.zip/download';" ^
  "Write-Host 'Lade MaryTTS herunter...';" ^
  "Invoke-WebRequest -Uri $url -OutFile $zip;" ^
  "Write-Host 'Entpacke MaryTTS...';" ^
  "Expand-Archive -Path $zip -DestinationPath $tmp -Force;" ^
  "$launcher=Get-ChildItem -Path $tmp -Recurse -File -Filter 'marytts-server.bat' | Select-Object -First 1;" ^
  "if(-not $launcher){ $launcher=Get-ChildItem -Path $tmp -Recurse -File -Filter 'marytts.bat' | Select-Object -First 1 };" ^
  "if(-not $launcher){ throw 'Kein MaryTTS-Launcher im Download gefunden.' };" ^
  "$home=$launcher.Directory.Parent.FullName;" ^
  "Remove-Item $target -Recurse -Force -ErrorAction SilentlyContinue;" ^
  "New-Item -ItemType Directory -Path $target -Force | Out-Null;" ^
  "Copy-Item -Path (Join-Path $home '*') -Destination $target -Recurse -Force;" ^
  "Write-Host 'MaryTTS installiert unter:' $target;" ^
  "Write-Host 'Launcher:' (Join-Path $target 'bin\marytts-server.bat');"

if errorlevel 1 (
  echo.
  echo MaryTTS-Installation fehlgeschlagen.
  echo Browser-TTS funktioniert weiterhin.
  pause
  exit /b 1
)

echo.
echo Fertig. Starte danach Backend neu.
pause
