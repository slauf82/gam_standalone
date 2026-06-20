$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$PiperDir = Join-Path $Root "tts\piper"
$VoiceDir = Join-Path $PiperDir "voices"
$TempDir = Join-Path $Root "tts\_download"

$PiperZipUrl = "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip"
$PiperZip = Join-Path $TempDir "piper_windows_amd64.zip"

New-Item -ItemType Directory -Force -Path $PiperDir | Out-Null
New-Item -ItemType Directory -Force -Path $VoiceDir | Out-Null
New-Item -ItemType Directory -Force -Path $TempDir | Out-Null

function Download-File($Url, $Target) {
  Write-Host "[GAM] Download:" $Url
  $headers = @{ "User-Agent" = "GAM-PiperTTS-Installer" }
  Invoke-WebRequest -Uri $Url -OutFile $Target -Headers $headers -MaximumRedirection 10
  if (!(Test-Path $Target) -or ((Get-Item $Target).Length -le 0)) {
    throw "Download fehlgeschlagen oder Datei leer: $Target"
  }
}

function Install-PiperEngine {
  $exe = Join-Path $PiperDir "piper.exe"
  $runtimeDll = Join-Path $PiperDir "onnxruntime.dll"
  $espeakData = Join-Path $PiperDir "espeak-ng-data"

  if ((Test-Path $exe) -and ((Test-Path $runtimeDll) -or (Test-Path $espeakData))) {
    Write-Host "[GAM] Piper Engine bereits vollstaendig vorhanden."
    return
  }

  Write-Host "[GAM] Piper Engine wird als komplettes Windows-Release-ZIP installiert."
  if (Test-Path $PiperZip) { Remove-Item $PiperZip -Force }
  Download-File $PiperZipUrl $PiperZip

  $extract = Join-Path $TempDir "piper_extract"
  if (Test-Path $extract) { Remove-Item $extract -Recurse -Force }
  New-Item -ItemType Directory -Force -Path $extract | Out-Null
  Expand-Archive -Path $PiperZip -DestinationPath $extract -Force

  $foundExe = Get-ChildItem -Path $extract -Filter "piper.exe" -Recurse | Select-Object -First 1
  if ($null -eq $foundExe) {
    throw "piper.exe wurde im Release-ZIP nicht gefunden."
  }

  $sourceDir = $foundExe.Directory.FullName
  Write-Host "[GAM] Kopiere Piper-Dateien aus:" $sourceDir
  Copy-Item -Path (Join-Path $sourceDir "*") -Destination $PiperDir -Recurse -Force

  if (!(Test-Path (Join-Path $PiperDir "piper.exe"))) {
    throw "piper.exe wurde nach dem Entpacken nicht gefunden."
  }
  if (!(Test-Path (Join-Path $PiperDir "onnxruntime.dll")) -and !(Test-Path (Join-Path $PiperDir "espeak-ng-data"))) {
    throw "Piper-Begleitdateien fehlen nach dem Entpacken. Erwartet: onnxruntime.dll und/oder espeak-ng-data."
  }

  Write-Host "[GAM] Piper Engine installiert."
}

Install-PiperEngine

$voices = @(
  @{ id="de_DE-thorsten-medium"; base="https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/medium/de_DE-thorsten-medium" },
  @{ id="en_US-lessac-medium"; base="https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium" },
  @{ id="fr_FR-siwis-medium"; base="https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/medium/fr_FR-siwis-medium" }
)

foreach ($v in $voices) {
  $onnx = Join-Path $VoiceDir ($v.id + ".onnx")
  $json = Join-Path $VoiceDir ($v.id + ".onnx.json")
  if (!(Test-Path $onnx)) {
    Write-Host "[GAM] Lade" $v.id ".onnx"
    Download-File ($v.base + ".onnx") $onnx
  } else {
    Write-Host "[GAM] Bereits vorhanden:" $onnx
  }
  if (!(Test-Path $json)) {
    Write-Host "[GAM] Lade" $v.id ".onnx.json"
    Download-File ($v.base + ".onnx.json") $json
  } else {
    Write-Host "[GAM] Bereits vorhanden:" $json
  }
}

# Echttest: Piper ist erst verfuegbar, wenn eine WAV-Datei erzeugt werden kann.
$testWav = Join-Path $PiperDir "piper-install-test.wav"
if (Test-Path $testWav) { Remove-Item $testWav -Force }
$testModel = Join-Path $VoiceDir "de_DE-thorsten-medium.onnx"
$testText = "Hallo, dies ist ein Piper Test."
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = Join-Path $PiperDir "piper.exe"
$psi.Arguments = "--model `"$testModel`" --output_file `"$testWav`""
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$p = [System.Diagnostics.Process]::Start($psi)
$p.StandardInput.WriteLine($testText)
$p.StandardInput.Close()
$stdout = $p.StandardOutput.ReadToEnd()
$stderr = $p.StandardError.ReadToEnd()
$p.WaitForExit()
if ($p.ExitCode -ne 0 -or !(Test-Path $testWav) -or ((Get-Item $testWav).Length -le 44)) {
  Write-Host $stdout
  Write-Host $stderr
  throw "Piper-Echttest fehlgeschlagen. ExitCode=$($p.ExitCode)"
}

Write-Host "[GAM] Piper Echttest erfolgreich:" $testWav
Write-Host "[GAM] Standardstimmen installiert."
