<#
  GAM 2.0 v1.7.2 / Schritt 36h
  Sicheres Backend-Datenbank-Setup fuer Windows.

  Ziel:
  - vorhandene MariaDB/MySQL unter localhost:3306 respektieren
  - keine vorhandenen Datenbanken ueberschreiben
  - bei fehlender DB optional portable MariaDB unter tools\mariadb vorbereiten
  - Demo-Datenbank nur importieren, wenn die Ziel-DB fehlt oder leer ist
#>
param(
  [string]$DatabaseName = $(if ($env:GAM_DB_NAME) { $env:GAM_DB_NAME } else { "kopfzentruminventardb" }),
  [string]$DbUser = $(if ($env:GAM_DB_USER) { $env:GAM_DB_USER } else { "root" }),
  [string]$DbPassword = $(if ($env:GAM_DB_PASSWORD) { $env:GAM_DB_PASSWORD } else { "" }),
  [int]$Port = $(if ($env:GAM_DB_PORT) { [int]$env:GAM_DB_PORT } else { 3306 }),
  [string]$SqlFile = $(if ($env:GAM_DEMO_SQL) { $env:GAM_DEMO_SQL } else { "sql\beispiel_v1_7_0_anonymisiert.sql" })
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot ".."))
$ToolsDir = Join-Path $Root "tools"
$MariaRoot = Join-Path $ToolsDir "mariadb"
$PortableBin = Join-Path $MariaRoot "bin"
$PortableData = Join-Path $MariaRoot "data"
$SqlPath = Join-Path $Root $SqlFile
$LogDir = Join-Path $Root "logs"
$LogFile = Join-Path $LogDir "mariadb-portable.log"

function Write-Info($msg) { Write-Host "[GAM-DB] $msg" }
function Write-Warn($msg) { Write-Host "[GAM-DB][WARNUNG] $msg" -ForegroundColor Yellow }
function Write-Err($msg) { Write-Host "[GAM-DB][FEHLER] $msg" -ForegroundColor Red }

function Test-PortOpen {
  param([string]$HostName = "127.0.0.1", [int]$PortNumber = 3306)
  try {
    $client = New-Object System.Net.Sockets.TcpClient
    $iar = $client.BeginConnect($HostName, $PortNumber, $null, $null)
    $ok = $iar.AsyncWaitHandle.WaitOne(800, $false)
    if ($ok) { $client.EndConnect($iar) | Out-Null }
    $client.Close()
    return $ok
  } catch { return $false }
}

function Find-DbClient {
  $candidates = @(
    (Join-Path $PortableBin "mariadb.exe"),
    (Join-Path $PortableBin "mysql.exe"),
    "mariadb.exe",
    "mysql.exe"
  )
  foreach ($c in $candidates) {
    try {
      $cmd = Get-Command $c -ErrorAction SilentlyContinue
      if ($cmd) { return $cmd.Source }
    } catch {}
    if (Test-Path $c) { return $c }
  }
  return $null
}

function Find-DbServerBinary {
  $candidates = @(
    (Join-Path $PortableBin "mariadbd.exe"),
    (Join-Path $PortableBin "mysqld.exe")
  )
  foreach ($c in $candidates) {
    if (Test-Path $c) { return $c }
  }
  return $null
}

function Invoke-DbClient {
  param([string]$Client, [string]$Sql, [switch]$UseDatabase)
  $args = @("-h", "127.0.0.1", "-P", "$Port", "-u", $DbUser, "--default-character-set=utf8mb4", "--batch", "--silent")
  if ($DbPassword -ne "") { $args += "-p$DbPassword" }
  if ($UseDatabase) { $args += $DatabaseName }
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $Client
  foreach ($a in $args) { [void]$psi.ArgumentList.Add($a) }
  $psi.RedirectStandardInput = $true
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.UseShellExecute = $false
  $p = [System.Diagnostics.Process]::Start($psi)
  $p.StandardInput.WriteLine($Sql)
  $p.StandardInput.Close()
  $out = $p.StandardOutput.ReadToEnd()
  $err = $p.StandardError.ReadToEnd()
  $p.WaitForExit()
  if ($p.ExitCode -ne 0) { throw "DB-Client Fehler ($($p.ExitCode)): $err" }
  return $out.Trim()
}

function Import-SqlFile {
  param([string]$Client, [string]$FilePath)
  if (!(Test-Path $FilePath)) {
    Write-Warn "Demo-SQL nicht gefunden: $FilePath"
    return $false
  }
  $args = @("-h", "127.0.0.1", "-P", "$Port", "-u", $DbUser, "--default-character-set=utf8mb4", $DatabaseName)
  if ($DbPassword -ne "") { $args = @("-h", "127.0.0.1", "-P", "$Port", "-u", $DbUser, "-p$DbPassword", "--default-character-set=utf8mb4", $DatabaseName) }
  $cmdLine = "`"$Client`" " + ($args | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }) -join ' '
  Write-Info "Importiere Demo-SQL nach '$DatabaseName'..."
  $p = Start-Process -FilePath "cmd.exe" -ArgumentList "/c $cmdLine < `"$FilePath`"" -Wait -PassThru -NoNewWindow
  if ($p.ExitCode -ne 0) { throw "SQL-Import fehlgeschlagen (ExitCode $($p.ExitCode))." }
  return $true
}

function Download-PortableMariaDB {
  if (!(Test-Path $ToolsDir)) { New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null }
  $zipPath = Join-Path $ToolsDir "mariadb-winx64.zip"
  $urls = @()
  if ($env:GAM_MARIADB_ZIP_URL) { $urls += $env:GAM_MARIADB_ZIP_URL }
  # LTS-Release bevorzugen; falls MariaDB die Struktur aendert, kann GAM_MARIADB_ZIP_URL in .env gesetzt werden.
  $urls += "https://archive.mariadb.org/mariadb-11.4.12/winx64-packages/mariadb-11.4.12-winx64.zip"
  $urls += "https://downloads.mariadb.com/MariaDB/mariadb-11.4.12/winx64-packages/mariadb-11.4.12-winx64.zip"
  foreach ($url in $urls) {
    try {
      Write-Info "Lade portable MariaDB: $url"
      Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing -TimeoutSec 120
      if ((Test-Path $zipPath) -and ((Get-Item $zipPath).Length -gt 20000000)) { return $zipPath }
    } catch {
      Write-Warn "Download fehlgeschlagen: $($_.Exception.Message)"
    }
  }
  throw "Portable MariaDB konnte nicht automatisch heruntergeladen werden. Setze optional GAM_MARIADB_ZIP_URL in .env."
}

function Install-PortableMariaDB {
  if (Test-Path (Join-Path $PortableBin "mariadbd.exe")) {
    Write-Info "Portable MariaDB-Dateien bereits vorhanden."
  } else {
    $zipPath = Download-PortableMariaDB
    $extractDir = Join-Path $ToolsDir "mariadb_extract"
    if (Test-Path $extractDir) { Remove-Item -Recurse -Force $extractDir }
    New-Item -ItemType Directory -Force -Path $extractDir | Out-Null
    Write-Info "Entpacke MariaDB ZIP..."
    Expand-Archive -Path $zipPath -DestinationPath $extractDir -Force
    $rootCandidate = Get-ChildItem -Path $extractDir -Directory | Select-Object -First 1
    if (!$rootCandidate) { throw "MariaDB ZIP konnte nicht korrekt entpackt werden." }
    if (Test-Path $MariaRoot) { Remove-Item -Recurse -Force $MariaRoot }
    Move-Item -Path $rootCandidate.FullName -Destination $MariaRoot
    Remove-Item -Recurse -Force $extractDir
  }

  if (!(Test-Path $PortableData) -or -not (Get-ChildItem -Path $PortableData -ErrorAction SilentlyContinue | Select-Object -First 1)) {
    Write-Info "Initialisiere portable MariaDB-Datenverzeichnis..."
    $installDb = Join-Path $PortableBin "mariadb-install-db.exe"
    $mysqlInstallDb = Join-Path $PortableBin "mysql_install_db.exe"
    if (Test-Path $installDb) {
      & $installDb "--datadir=$PortableData" | Write-Host
      if ($LASTEXITCODE -ne 0) { throw "mariadb-install-db.exe fehlgeschlagen." }
    } elseif (Test-Path $mysqlInstallDb) {
      & $mysqlInstallDb "--datadir=$PortableData" | Write-Host
      if ($LASTEXITCODE -ne 0) { throw "mysql_install_db.exe fehlgeschlagen." }
    } else {
      throw "Kein Install-DB-Werkzeug im MariaDB-Paket gefunden."
    }
  }

  $server = Find-DbServerBinary
  if (!$server) { throw "mariadbd.exe/mysqld.exe nicht gefunden." }
  if (!(Test-Path $LogDir)) { New-Item -ItemType Directory -Force -Path $LogDir | Out-Null }
  Write-Info "Starte portable MariaDB auf Port $Port..."
  $args = @("--datadir=$PortableData", "--port=$Port", "--bind-address=127.0.0.1", "--console", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
  Start-Process -FilePath $server -ArgumentList $args -WorkingDirectory $MariaRoot -RedirectStandardOutput $LogFile -RedirectStandardError $LogFile -WindowStyle Minimized | Out-Null
  for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    if (Test-PortOpen -PortNumber $Port) { Write-Info "Portable MariaDB ist erreichbar."; return $true }
  }
  throw "Portable MariaDB wurde gestartet, ist aber nicht erreichbar. Log: $LogFile"
}

Write-Info "Pruefe MariaDB/MySQL unter localhost:$Port..."
$serverAvailable = Test-PortOpen -PortNumber $Port

if (!$serverAvailable) {
  Write-Warn "Keine MariaDB/MySQL-Instanz unter localhost:$Port erkannt."
  $auto = $env:GAM_DB_AUTO_SETUP
  if (!$auto) {
    $answer = Read-Host "Portable MariaDB fuer GAM lokal unter tools\mariadb einrichten? [J/N]"
    if ($answer -match '^[JjYy]') { $auto = "true" } else { $auto = "false" }
  }
  if ($auto -eq "true" -or $auto -eq "1" -or $auto -eq "yes") {
    Install-PortableMariaDB | Out-Null
    $serverAvailable = Test-PortOpen -PortNumber $Port
  } else {
    Write-Warn "DB-Setup uebersprungen. Backend kann ohne erreichbare Datenbank nicht vollstaendig starten."
    exit 0
  }
} else {
  Write-Info "MariaDB/MySQL erreichbar. Vorhandene Installation wird respektiert."
}

$client = Find-DbClient
if (!$client) {
  Write-Warn "Kein mariadb.exe/mysql.exe gefunden. DB-Erreichbarkeit ist ok, Demo-Import wird uebersprungen."
  exit 0
}

try {
  Write-Info "Pruefe Ziel-Datenbank '$DatabaseName'..."
  Invoke-DbClient -Client $client -Sql "CREATE DATABASE IF NOT EXISTS \`$DatabaseName\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
  $count = Invoke-DbClient -Client $client -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DatabaseName';"
  if ([int]$count -eq 0) {
    Import-SqlFile -Client $client -FilePath $SqlPath | Out-Null
    Write-Info "Demo-Datenbank wurde importiert."
  } else {
    Write-Info "Datenbank '$DatabaseName' enthaelt bereits Tabellen ($count). Kein Import, kein Ueberschreiben."
  }
} catch {
  Write-Warn "Demo-Import konnte nicht abgeschlossen werden: $($_.Exception.Message)"
  Write-Warn "Backend-Start wird nicht blockiert. Bitte DB-Zugang in .env pruefen."
}
