$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$RuntimeDir = Join-Path $Root 'runtime\java'
$SelectionFile = Join-Path $Root 'runtime\java-home.txt'
$RequiredMajor = 21

function Get-MajorFromVersionText([string]$VersionText) {
    if ([string]::IsNullOrWhiteSpace($VersionText)) { return 0 }
    $text = $VersionText.Trim().Trim('"')
    if ($text -match '^1\.(?<legacy>[0-9]+)') { return [int]$Matches.legacy }
    if ($text -match '^(?<major>[0-9]+)') { return [int]$Matches.major }
    if ($text -match '(?:version|javac)\s+"?(?<major>[0-9]+)') { return [int]$Matches.major }
    return 0
}

function Get-JavaMajor([string]$Executable) {
    if ([string]::IsNullOrWhiteSpace($Executable) -or -not (Test-Path -LiteralPath $Executable)) { return 0 }
    try {
        $output = (& $Executable -version 2>&1 | ForEach-Object { $_.ToString() }) -join "`n"
        return Get-MajorFromVersionText $output
    } catch { }
    return 0
}

function Get-JdkReleaseMajor([string]$JdkHomePath) {
    $releaseFile = Join-Path $JdkHomePath 'release'
    if (-not (Test-Path -LiteralPath $releaseFile)) { return 0 }
    try {
        $line = Get-Content -LiteralPath $releaseFile -ErrorAction Stop |
            Where-Object { $_ -match '^JAVA_VERSION=' } |
            Select-Object -First 1
        if ($line -match '^JAVA_VERSION="?(?<version>[^"\r\n]+)') {
            return Get-MajorFromVersionText $Matches.version
        }
    } catch { }
    return 0
}

function Test-JdkHome([string]$JdkHomePath) {
    if ([string]::IsNullOrWhiteSpace($JdkHomePath)) { return $null }
    $expanded = [Environment]::ExpandEnvironmentVariables($JdkHomePath.Trim().Trim('"'))

    # Auch Eingaben wie ...\bin oder ...\bin\java.exe tolerant auf das JDK-Home zurückführen.
    if (Test-Path -LiteralPath $expanded -PathType Leaf) {
        $expanded = Split-Path -Parent (Split-Path -Parent $expanded)
    } elseif ((Split-Path -Leaf $expanded) -ieq 'bin') {
        $expanded = Split-Path -Parent $expanded
    }

    try { $resolved = (Resolve-Path -LiteralPath $expanded -ErrorAction Stop).Path } catch { return $null }
    $java = Join-Path $resolved 'bin\java.exe'
    $javac = Join-Path $resolved 'bin\javac.exe'
    if (-not (Test-Path -LiteralPath $java) -or -not (Test-Path -LiteralPath $javac)) { return $null }

    # Die standardisierte JDK-Datei "release" ist unter Windows zuverlässiger als das Parsen nativer stderr-Ausgaben.
    $releaseMajor = Get-JdkReleaseMajor $resolved
    $javaMajor = Get-JavaMajor $java
    $javacMajor = Get-JavaMajor $javac
    $detectedMajor = [Math]::Max($releaseMajor, [Math]::Max($javaMajor, $javacMajor))

    if ($detectedMajor -ge $RequiredMajor) {
        return [pscustomobject]@{
            JdkHome = $resolved
            JavaMajor = $detectedMajor
            JavacMajor = $(if ($javacMajor -gt 0) { $javacMajor } else { $detectedMajor })
            Detection = $(if ($releaseMajor -ge $RequiredMajor) { 'release' } else { 'command' })
        }
    }
    return $null
}

function Add-Candidate([System.Collections.Generic.List[string]]$List, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { return }
    $clean = [Environment]::ExpandEnvironmentVariables($Value.Trim().Trim('"'))
    if (-not $List.Contains($clean)) { $List.Add($clean) }
}

$candidates = [System.Collections.Generic.List[string]]::new()

# 1. Bereits von GAM lokal eingerichtetes JDK.
Add-Candidate $candidates $RuntimeDir

# 2. Offiziell gesetztes JAVA_HOME – der wichtigste Fundweg für vorhandene JDKs.
Add-Candidate $candidates $env:JAVA_HOME

# 3. java/javac aus PATH; aus deren bin-Verzeichnis wird das JDK-Home abgeleitet.
foreach ($commandName in @('javac', 'java')) {
    $command = Get-Command $commandName -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command -and $command.Source) {
        $binDir = Split-Path -Parent $command.Source
        Add-Candidate $candidates (Split-Path -Parent $binDir)
    }
}

# 4. Windows-Java-Registry (Oracle/OpenJDK-kompatible Installer).
$registryRoots = @(
    'HKLM:\SOFTWARE\JavaSoft\JDK',
    'HKLM:\SOFTWARE\JavaSoft\Java Development Kit',
    'HKLM:\SOFTWARE\WOW6432Node\JavaSoft\JDK',
    'HKLM:\SOFTWARE\WOW6432Node\JavaSoft\Java Development Kit'
)
foreach ($rootKey in $registryRoots) {
    if (Test-Path $rootKey) {
        Get-ChildItem $rootKey -ErrorAction SilentlyContinue | ForEach-Object {
            try { Add-Candidate $candidates ((Get-ItemProperty $_.PSPath -ErrorAction Stop).JavaHome) } catch { }
        }
    }
}

# 5. Übliche Installationsordner verschiedener JDK-Distributionen.
$searchRoots = @(
    "$env:ProgramW6432\Java",
    "$env:ProgramFiles\Java",
    "C:\Program Files\Java",
    "${env:ProgramFiles(x86)}\Java",
    "C:\Program Files (x86)\Java",
    "$env:ProgramFiles\Eclipse Adoptium",
    "$env:ProgramFiles\Microsoft",
    "$env:ProgramFiles\Amazon Corretto",
    "$env:ProgramFiles\Zulu",
    "$env:ProgramFiles\BellSoft",
    "$env:LOCALAPPDATA\Programs\Eclipse Adoptium"
)
# 5a. Exakte Standardpfade zuerst prüfen. Das deckt u. a. C:\Program Files\Java\jdk-21.0.10
# und C:\Program Files (x86)\Java\jdk-21.0.10 zuverlässig ab.
foreach ($basePath in @('C:\Program Files\Java', 'C:\Program Files (x86)\Java')) {
    if (Test-Path -LiteralPath $basePath) {
        Get-ChildItem -LiteralPath $basePath -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^jdk[-_]?21(?:[._-].*)?$' -or $_.Name -match '^jdk-21' } |
            Sort-Object Name -Descending |
            ForEach-Object { Add-Candidate $candidates $_.FullName }
    }
}

# 5a. Oracle/OpenJDK-Standardlayout ausdrücklich prüfen, z. B. C:\Program Files\Java\jdk-21.0.10.
foreach ($javaRoot in @("$env:ProgramW6432\Java", "$env:ProgramFiles\Java", "C:\Program Files\Java", "${env:ProgramFiles(x86)}\Java", "C:\Program Files (x86)\Java")) {
    if (Test-Path -LiteralPath $javaRoot) {
        Get-ChildItem -LiteralPath $javaRoot -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | ForEach-Object { Add-Candidate $candidates $_.FullName }
    }
}

foreach ($searchRoot in $searchRoots) {
    if (Test-Path -LiteralPath $searchRoot) {
        Get-ChildItem -LiteralPath $searchRoot -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            Add-Candidate $candidates $_.FullName
            Get-ChildItem -LiteralPath $_.FullName -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                Add-Candidate $candidates $_.FullName
            }
        }
    }
}

$selected = $null
foreach ($candidate in $candidates) {
    $validated = Test-JdkHome $candidate
    if ($validated) {
        $selected = $validated
        break
    }
}

if ($selected) {
    New-Item -ItemType Directory -Force -Path (Split-Path $SelectionFile -Parent) | Out-Null
    Set-Content -LiteralPath $SelectionFile -Value $selected.JdkHome -Encoding ASCII
    Write-Host "[GAM] JDK $($selected.JavaMajor) gefunden: $($selected.JdkHome) (Erkennung: $($selected.Detection))"
    exit 0
}

Write-Host "[GAM] Kein geeignetes JDK $RequiredMajor gefunden. Eclipse Temurin wird lokal fuer GAM heruntergeladen..."
New-Item -ItemType Directory -Force -Path (Split-Path $RuntimeDir -Parent) | Out-Null
$temp = Join-Path $env:TEMP ("gam-java-" + [guid]::NewGuid().ToString('N'))
$archive = "$temp.zip"
New-Item -ItemType Directory -Force -Path $temp | Out-Null

try {
    $architecture = if ([Environment]::Is64BitOperatingSystem) { 'x64' } else { 'x32' }
    $url = "https://api.adoptium.net/v3/binary/latest/$RequiredMajor/ga/windows/$architecture/jdk/hotspot/normal/eclipse?project=jdk"
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $archive
    Expand-Archive -Path $archive -DestinationPath $temp -Force
    $javaRoot = Get-ChildItem -Path $temp -Directory -Recurse | Where-Object {
        (Test-Path (Join-Path $_.FullName 'bin\java.exe')) -and (Test-Path (Join-Path $_.FullName 'bin\javac.exe'))
    } | Select-Object -First 1
    if (-not $javaRoot) { throw 'Das heruntergeladene Java-Archiv enthaelt kein nutzbares JDK.' }
    if (Test-Path $RuntimeDir) { Remove-Item -Recurse -Force $RuntimeDir }
    Move-Item -Path $javaRoot.FullName -Destination $RuntimeDir
    $installed = Test-JdkHome $RuntimeDir
    if (-not $installed) { throw 'Das lokale JDK konnte nicht validiert werden.' }
    Set-Content -LiteralPath $SelectionFile -Value $installed.JdkHome -Encoding ASCII
    Write-Host "[GAM] JDK $($installed.JavaMajor) wurde lokal unter runtime\java eingerichtet."
} finally {
    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
    Remove-Item -Force $archive -ErrorAction SilentlyContinue
}
