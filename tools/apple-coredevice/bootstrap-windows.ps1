param(
  [Parameter(Mandatory=$true)][string]$GamRoot,
  [Parameter(Mandatory=$false)][string]$Udid = "",
  [Parameter(Mandatory=$false)][switch]$UvOnly
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# GAM 40k37e21: isolierte CoreDevice-Runtime fuer Windows.
# e18: native Programme werden ueber Start-Process ausgefuehrt. Normale stderr-Ausgaben
# von uv/pymobiledevice3 sind dadurch keine PowerShell NativeCommandError-Abbrueche mehr.
# Erfolg entscheidet ausschliesslich der echte Prozess-Exitcode.
$PinnedUv = '0.12.13'
$PinnedPmd3 = '11.16.3'
$PinnedPython = '3.13'
$core = Join-Path $GamRoot 'tools\apple-coredevice'
$runtime = Join-Path $core 'runtime'
$uvDir = Join-Path $runtime 'uv'
$uvExe = Join-Path $uvDir 'uv.exe'
$uvZip = Join-Path $runtime "uv-$PinnedUv-x64.zip"
$binDir = Join-Path $runtime 'bin'
$cacheDir = Join-Path $runtime 'cache'
$pythonDir = Join-Path $runtime 'python'
$toolDir = Join-Path $runtime 'tools'
$nativeLogDir = Join-Path $runtime 'native-logs'
New-Item -ItemType Directory -Force -Path $runtime,$uvDir,$binDir,$cacheDir,$pythonDir,$toolDir,$nativeLogDir | Out-Null

$env:UV_CACHE_DIR = $cacheDir
$env:UV_PYTHON_INSTALL_DIR = $pythonDir
$env:UV_TOOL_DIR = $toolDir
$env:UV_TOOL_BIN_DIR = $binDir
$env:UV_NO_MODIFY_PATH = '1'
$env:PYTHONNOUSERSITE = '1'
$env:PYMOBILEDEVICE3_USERSPACE_TCP_RELAY = '1'

function Invoke-NativeE20 {
  param(
    [Parameter(Mandatory=$true)][string]$FilePath,
    [Parameter(Mandatory=$false)][string[]]$ArgumentList = @(),
    [Parameter(Mandatory=$true)][string]$Label,
    [Parameter(Mandatory=$false)][int]$TimeoutSeconds = 30
  )
  $token = [Guid]::NewGuid().ToString('N')
  $stdoutFile = Join-Path $nativeLogDir "$token.stdout.txt"
  $stderrFile = Join-Path $nativeLogDir "$token.stderr.txt"
  $started = Get-Date
  [Console]::Out.WriteLine("$Label START file=$FilePath timeoutSeconds=$TimeoutSeconds")
  try {
    $p = Start-Process -FilePath $FilePath -ArgumentList $ArgumentList -NoNewWindow -PassThru `
      -RedirectStandardOutput $stdoutFile -RedirectStandardError $stderrFile
    [Console]::Out.WriteLine("$Label PID=$($p.Id)")
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while (!$p.HasExited -and (Get-Date) -lt $deadline) {
      Start-Sleep -Milliseconds 200
      $p.Refresh()
    }
    if (!$p.HasExited) {
      [Console]::Out.WriteLine("$Label TIMEOUT pid=$($p.Id) timeoutSeconds=$TimeoutSeconds action=kill-tree")
      try { & taskkill.exe /PID $p.Id /T /F 2>&1 | ForEach-Object { [Console]::Out.WriteLine("$Label taskkill=$_") } } catch {}
      try { if (!$p.HasExited) { $p.Kill() } } catch {}
      try { $p.WaitForExit(3000) } catch {}
    } else {
      try { $p.WaitForExit() } catch {}
    }
    $stdout = if (Test-Path $stdoutFile) { [IO.File]::ReadAllText($stdoutFile).Trim() } else { '' }
    $stderr = if (Test-Path $stderrFile) { [IO.File]::ReadAllText($stderrFile).Trim() } else { '' }
    if ($stdout) { $stdout -split "`r?`n" | ForEach-Object { [Console]::Out.WriteLine("$Label stdout=$_") } }
    if ($stderr) { $stderr -split "`r?`n" | ForEach-Object { [Console]::Out.WriteLine("$Label stderr=$_") } }
    $elapsed = [int]((Get-Date) - $started).TotalMilliseconds
    if (!$p.HasExited) {
      [Console]::Out.WriteLine("$Label END exit=124 timedOut=true durationMs=$elapsed")
      return [pscustomobject]@{ ExitCode=124; Stdout=$stdout; Stderr=$stderr; TimedOut=$true; DurationMs=$elapsed }
    }
    $exitCode = [int]$p.ExitCode
    [Console]::Out.WriteLine("$Label END exit=$exitCode timedOut=false durationMs=$elapsed")
    return [pscustomobject]@{ ExitCode=$exitCode; Stdout=$stdout; Stderr=$stderr; TimedOut=$false; DurationMs=$elapsed }
  } finally {
    Remove-Item -Force -ErrorAction SilentlyContinue $stdoutFile,$stderrFile
  }
}

if (!(Test-Path $uvExe)) {
  $base = "https://releases.astral.sh/github/uv/releases/download/$PinnedUv"
  $asset = 'uv-x86_64-pc-windows-msvc.zip'
  $hashAsset = "$asset.sha256"
  Write-Output "E20_BOOTSTRAP uv=missing action=download version=$PinnedUv"
  Invoke-WebRequest -UseBasicParsing -Uri "$base/$asset" -OutFile $uvZip
  $hashResponse = Invoke-WebRequest -UseBasicParsing -Uri "$base/$hashAsset"
  $hashContent = $hashResponse.Content
  if ($hashContent -is [byte[]]) { $hashText = [Text.Encoding]::UTF8.GetString($hashContent).Trim() }
  else { $hashText = ([string]$hashContent).Trim() }
  $expected = (($hashText -split '\s+')[0]).ToLowerInvariant()
  $actual = (Get-FileHash -Algorithm SHA256 $uvZip).Hash.ToLowerInvariant()
  if ([string]::IsNullOrWhiteSpace($expected) -or $expected -ne $actual) {
    Remove-Item -Force -ErrorAction SilentlyContinue $uvZip
    throw "uv SHA256 mismatch: expected=$expected actual=$actual"
  }
  Expand-Archive -Force -Path $uvZip -DestinationPath $uvDir
  Remove-Item -Force -ErrorAction SilentlyContinue $uvZip
}

if ($UvOnly) {
  Write-Output "E20_UV_ONLY ready=true uv=$uvExe"
  exit 0
}

$uvVersionResult = Invoke-NativeE20 -FilePath $uvExe -ArgumentList @('--version') -Label 'E20_UV_VERSION' -TimeoutSeconds 15
if ($uvVersionResult.ExitCode -ne 0) { throw "uv --version failed: exit=$($uvVersionResult.ExitCode)" }
$uvVersion = $uvVersionResult.Stdout.Trim()
Write-Output "E20_BOOTSTRAP uv=$uvVersion root=$runtime"

$pmdExe = Join-Path $binDir 'pymobiledevice3.exe'
$marker = Join-Path $runtime "pymobiledevice3-$PinnedPmd3.ready"
if (!(Test-Path $pmdExe) -or !(Test-Path $marker)) {
  Write-Output "E20_BOOTSTRAP pymobiledevice3=missing action=install version=$PinnedPmd3 python=$PinnedPython"
  $install = Invoke-NativeE20 -FilePath $uvExe -ArgumentList @('tool','install','--force','--python',$PinnedPython,"pymobiledevice3==$PinnedPmd3") -Label 'E20_UV_INSTALL' -TimeoutSeconds 90
  if ($install.ExitCode -ne 0) { throw "uv tool install pymobiledevice3 failed: exit=$($install.ExitCode)" }
  if (!(Test-Path $pmdExe)) { throw "pymobiledevice3.exe missing after successful uv install: $pmdExe" }
  Set-Content -Encoding ASCII -Path $marker -Value "pymobiledevice3=$PinnedPmd3`npython=$PinnedPython`nuv=$PinnedUv"
}
if (!(Test-Path $pmdExe)) { throw "pymobiledevice3.exe missing after install: $pmdExe" }

$versionResult = Invoke-NativeE20 -FilePath $pmdExe -ArgumentList @('version') -Label 'E20_PMD3_VERSION' -TimeoutSeconds 15
if ($versionResult.ExitCode -ne 0) { throw "pymobiledevice3 version failed: exit=$($versionResult.ExitCode)" }
$version = $versionResult.Stdout.Trim()
Write-Output "E20_RUNTIME ready=true pymobiledevice3=$version python=$PinnedPython isolated=true"

# Read-only discovery only. remote browse performs no pairing, tunnel creation or device mutation.
if ($Udid) { $env:PYMOBILEDEVICE3_UDID = $Udid }
$browse = Invoke-NativeE20 -FilePath $pmdExe -ArgumentList @('remote','browse','--no-color') -Label 'E20_REMOTE_BROWSE_NATIVE' -TimeoutSeconds 20
$data = (($browse.Stdout + "`n" + $browse.Stderr).Trim() -replace "`r?`n", ' | ')
Write-Output "E20_REMOTE_BROWSE exit=$($browse.ExitCode) data=$data"
exit $browse.ExitCode
