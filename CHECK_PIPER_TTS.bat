@echo off
setlocal
cd /d "%~dp0"

echo [GAM] PiperTTS Check
if exist "tts\piper\piper.exe" (echo OK piper.exe gefunden) else (echo FEHLT tts\piper\piper.exe)
if exist "tts\piper\onnxruntime.dll" (echo OK onnxruntime.dll gefunden) else (echo HINWEIS onnxruntime.dll nicht gefunden)
if exist "tts\piper\espeak-ng-data" (echo OK espeak-ng-data gefunden) else (echo HINWEIS espeak-ng-data nicht gefunden)

echo.
echo Stimmen:
for %%L in (de_DE-thorsten-medium en_US-lessac-medium fr_FR-siwis-medium it_IT-paola-medium es_ES-sharvard-medium pt_PT-tugao-medium nl_NL-ronnie-medium pl_PL-gosia-medium cs_CZ-jirka-low sv_SE-nst-medium tr_TR-dfki-medium ru_RU-ruslan-medium uk_UA-ukrainian_tts-medium) do (
  if exist "tts\piper\voices\%%L.onnx" if exist "tts\piper\voices\%%L.onnx.json" (echo OK %%L) else (echo FEHLT %%L json)
)

echo.
echo Echttest mit deutscher Stimme:
if exist "tts\piper\test.wav" del "tts\piper\test.wav"
echo Hallo, dies ist ein Piper Test. | "tts\piper\piper.exe" --model "tts\piper\voices\de_DE-thorsten-medium.onnx" --output_file "tts\piper\test.wav"
if exist "tts\piper\test.wav" (
  echo OK test.wav wurde erzeugt: tts\piper\test.wav
) else (
  echo FEHLER test.wav wurde nicht erzeugt.
)

echo.
echo Backend-Status pruefen:
echo   http://localhost:8080/api/tts/status
pause
