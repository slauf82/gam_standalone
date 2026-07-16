$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$RuntimeDir = Join-Path $Root 'runtime\java'
$JavaExe = Join-Path $RuntimeDir 'bin\java.exe'
$JavacExe = Join-Path $RuntimeDir 'bin\javac.exe'
$RequiredMajor = 21

function Get-JavaMajor([string]$Executable) {
    if (-not (Test-Path $Executable)) { return 0 }
    try {
        $output = (& $Executable -version 2>&1 | Out-String)
        if ($output -match 'version\s+"(?<version>[0-9]+)(?:\.[0-9]+)?') {
            $major = [int]$Matches.version
            if ($major -eq 1 -and $output -match 'version\s+"1\.(?<legacy>[0-9]+)') {
                return [int]$Matches.legacy
            }
            return $major
        }
    } catch { }
    return 0
}

$systemJava = Get-Command java -ErrorAction SilentlyContinue
if ($systemJava) {
    $major = Get-JavaMajor $systemJava.Source
    $systemJavac = Get-Command javac -ErrorAction SilentlyContinue
    if ($major -ge $RequiredMajor -and $systemJavac) {
        Write-Host "[GAM] Java $major gefunden: $($systemJava.Source)"
        exit 0
    }
    Write-Host "[GAM] Vorhandenes Java $major ist zu alt. GAM richtet Java $RequiredMajor lokal ein."
}

$localMajor = Get-JavaMajor $JavaExe
if ($localMajor -ge $RequiredMajor -and (Test-Path $JavacExe)) {
    Write-Host "[GAM] Lokale Java-Laufzeit $localMajor ist vorhanden."
    exit 0
}

Write-Host "[GAM] Java $RequiredMajor fehlt. Eclipse Temurin wird lokal fuer GAM heruntergeladen..."
New-Item -ItemType Directory -Force -Path (Split-Path $RuntimeDir -Parent) | Out-Null
$temp = Join-Path $env:TEMP ("gam-java-" + [guid]::NewGuid().ToString('N'))
$archive = "$temp.zip"
New-Item -ItemType Directory -Force -Path $temp | Out-Null

try {
    $url = "https://api.adoptium.net/v3/binary/latest/$RequiredMajor/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $archive
    Expand-Archive -Path $archive -DestinationPath $temp -Force
    $javaRoot = Get-ChildItem -Path $temp -Directory | Where-Object { (Test-Path (Join-Path $_.FullName 'bin\java.exe')) -and (Test-Path (Join-Path $_.FullName 'bin\javac.exe')) } | Select-Object -First 1
    if (-not $javaRoot) { throw 'Das heruntergeladene Java-Archiv enthaelt keine nutzbare Laufzeit.' }
    if (Test-Path $RuntimeDir) { Remove-Item -Recurse -Force $RuntimeDir }
    Move-Item -Path $javaRoot.FullName -Destination $RuntimeDir
    $installedMajor = Get-JavaMajor $JavaExe
    if ($installedMajor -lt $RequiredMajor) { throw 'Die lokale Java-Laufzeit konnte nicht validiert werden.' }
    Write-Host "[GAM] Java $installedMajor wurde lokal unter runtime\java eingerichtet."
} finally {
    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
    Remove-Item -Force $archive -ErrorAction SilentlyContinue
}
