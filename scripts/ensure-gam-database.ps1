<#
  GAM 2.0 v1.7.2 / Schritt 36h
  Sicheres Backend-Datenbank-Setup fuer Windows.

  Ziel:
  - vorhandene MariaDB/MySQL unter localhost:3306 respektieren
  - keine vorhandenen Datenbanken ueberschreiben
  - bei fehlender DB optional portable MariaDB unter tools\mariadb vorbereiten
  - nur den Datenbankserver und eine leere Ziel-Datenbank bereitstellen
  - Schema- oder Demodatenimport vollständig dem grafischen Frontend-Assistenten überlassen
#>
param(
  [string]$DatabaseName = $(if ($env:GAM_DB_NAME) { $env:GAM_DB_NAME } else { "kopfzentruminventardb" }),
  [string]$DbUser = $(if ($env:GAM_DB_USER) { $env:GAM_DB_USER } else { "root" }),
  [string]$DbPassword = $(if ($env:GAM_DB_PASSWORD) { $env:GAM_DB_PASSWORD } else { "" }),
  [int]$Port = $(if ($env:GAM_DB_PORT) { [int]$env:GAM_DB_PORT } else { 3306 })
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot ".."))
$ToolsDir = Join-Path $Root "tools"
$MariaRoot = Join-Path $ToolsDir "mariadb"
$PortableBin = Join-Path $MariaRoot "bin"
$PortableData = Join-Path $MariaRoot "data"
$LogDir = Join-Path $Root "logs"
$LogFile = Join-Path $LogDir "mariadb-portable.log"
$SchemaFile = Join-Path $Root "database\gam_v2_1_0_preview2_empty.sql"

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


function Find-DbDump {
  $candidates = @(
    (Join-Path $PortableBin "mariadb-dump.exe"),
    (Join-Path $PortableBin "mysqldump.exe"),
    "mariadb-dump.exe",
    "mysqldump.exe"
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

function Invoke-DbDumpTables {
  param([string]$DumpClient, [string]$SourceDatabase, [string[]]$Tables)
  $args = @("-h", "127.0.0.1", "-P", "$Port", "-u", $DbUser, "--default-character-set=utf8mb4", "--no-data", "--skip-add-drop-table", "--skip-comments", "--skip-lock-tables")
  if ($DbPassword -ne "") { $args += "-p$DbPassword" }
  $args += $SourceDatabase
  $args += $Tables
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $DumpClient
  foreach ($a in $args) { [void]$psi.ArgumentList.Add($a) }
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.UseShellExecute = $false
  $p = [System.Diagnostics.Process]::Start($psi)
  $out = $p.StandardOutput.ReadToEnd()
  $err = $p.StandardError.ReadToEnd()
  $p.WaitForExit()
  if ($p.ExitCode -ne 0) { throw "DB-Dump Fehler ($($p.ExitCode)): $err" }
  return $out
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

function Invoke-DbFile {
  param([string]$Client, [string]$FilePath)
  if (!(Test-Path $FilePath)) { throw "SQL-Schemadatei fehlt: $FilePath" }
  $args = @("-h", "127.0.0.1", "-P", "$Port", "-u", $DbUser, "--default-character-set=utf8mb4")
  if ($DbPassword -ne "") { $args += "-p$DbPassword" }
  $args += $DatabaseName
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $Client
  foreach ($a in $args) { [void]$psi.ArgumentList.Add($a) }
  $psi.RedirectStandardInput = $true
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.UseShellExecute = $false
  $p = [System.Diagnostics.Process]::Start($psi)
  $reader = [System.IO.File]::OpenText($FilePath)
  try {
    $buffer = New-Object char[] 65536
    while (($read = $reader.Read($buffer, 0, $buffer.Length)) -gt 0) {
      $p.StandardInput.Write($buffer, 0, $read)
    }
  } finally {
    $reader.Dispose()
    $p.StandardInput.Close()
  }
  $out = $p.StandardOutput.ReadToEnd()
  $err = $p.StandardError.ReadToEnd()
  $p.WaitForExit()
  if ($p.ExitCode -ne 0) { throw "Schemaimport fehlgeschlagen ($($p.ExitCode)): $err" }
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
  if (!$auto) { $auto = "true" }
  Write-Info "Portable MariaDB wird fuer den gefuehrten Erststart automatisch vorbereitet."
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
  Write-Warn "MariaDB/MySQL ist erreichbar, aber kein lokaler DB-Client (mariadb.exe/mysql.exe) wurde gefunden."
  Write-Info "Schema-Validierung wird uebersprungen. Das Backend verbindet sich direkt per JDBC (eigener Treiber im Backend enthalten)."
  exit 0
}

try {
  Write-Info "Bereite und validiere Ziel-Datenbank '$DatabaseName'..."
  Invoke-DbClient -Client $client -Sql "CREATE DATABASE IF NOT EXISTS ``$DatabaseName`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
  if (!(Test-Path $SchemaFile)) { throw "SQL-Schemadatei fehlt: $SchemaFile" }

  $schemaText = Get-Content -Raw -Path $SchemaFile
  $expectedTables = [regex]::Matches($schemaText, '(?im)^CREATE TABLE `([^`]+)`') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
  if (!$expectedTables -or $expectedTables.Count -eq 0) { throw "Aus der Schemadatei konnten keine erwarteten Tabellen ermittelt werden." }

  $missing = New-Object System.Collections.Generic.List[string]
  foreach ($table in $expectedTables) {
    $safeTable = $table.Replace("'", "''")
    $exists = Invoke-DbClient -Client $client -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DatabaseName' AND table_name='$safeTable';"
    if ([int]$exists -ne 1) { $missing.Add($table) }
  }

  if ($missing.Count -gt 0) {
    Write-Warn "Datenbankschema ist unvollstaendig. Fehlende Tabellen: $($missing -join ', ')"
    # Reparatur direkt in der GAM-Datenbank. Der GAM-Benutzer benoetigt
    # dadurch keine globalen CREATE-/DROP-DATABASE-Rechte. Es werden nur die
    # CREATE- und ALTER-Anweisungen der tatsaechlich fehlenden Tabellen ausgefuehrt.
    $repairSql = New-Object System.Text.StringBuilder
    [void]$repairSql.AppendLine('SET FOREIGN_KEY_CHECKS=0;')
    $selectedCount = 0
    foreach ($table in $missing) {
      $escaped = [regex]::Escape($table)
      $createPattern = "(?is)CREATE TABLE(?: IF NOT EXISTS)? ``$escaped``.*?;"
      $createMatch = [regex]::Match($schemaText, $createPattern)
      if (!$createMatch.Success) { throw "CREATE-TABLE-Anweisung fuer '$table' wurde im Basisschema nicht gefunden." }
      $createSql = $createMatch.Value -replace '(?i)^CREATE TABLE `', 'CREATE TABLE IF NOT EXISTS `'
      [void]$repairSql.AppendLine($createSql)
      $selectedCount++

      $alterPattern = "(?is)ALTER TABLE ``$escaped``.*?;"
      foreach ($alterMatch in [regex]::Matches($schemaText, $alterPattern)) {
        [void]$repairSql.AppendLine($alterMatch.Value)
        $selectedCount++
      }
    }
    [void]$repairSql.AppendLine('SET FOREIGN_KEY_CHECKS=1;')
    if ($selectedCount -eq 0) { throw "Aus dem Basisschema konnten keine Reparaturanweisungen erzeugt werden." }

    $repairFile = Join-Path ([System.IO.Path]::GetTempPath()) ("gam-schema-repair-{0}.sql" -f [guid]::NewGuid().ToString('N'))
    try {
      [System.IO.File]::WriteAllText($repairFile, $repairSql.ToString(), [System.Text.UTF8Encoding]::new($false))
      Write-Info "Trage fehlende Tabellen direkt in '$DatabaseName' nach ($selectedCount Schemaanweisungen, keine temporaere Datenbank erforderlich)..."
      Invoke-DbFile -Client $client -FilePath $repairFile
    } finally {
      Remove-Item -Force -ErrorAction SilentlyContinue $repairFile
    }
  } else {
    Write-Info "Alle $($expectedTables.Count) Tabellen aus dem GAM-Basisschema sind vorhanden."
  }

  Invoke-DbClient -Client $client -UseDatabase -Sql "CREATE TABLE IF NOT EXISTS gam_settings (setting_key VARCHAR(120) NOT NULL, setting_value TEXT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;" | Out-Null

  foreach ($table in $expectedTables) {
    $safeTable = $table.Replace("'", "''")
    $exists = Invoke-DbClient -Client $client -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DatabaseName' AND table_name='$safeTable';"
    if ([int]$exists -ne 1) { throw "Schema bleibt unvollstaendig; Tabelle '$table' fehlt." }
  }
  foreach ($core in @('accounts','news','rechnungsgesellschaft','gam_settings')) {
    $exists = Invoke-DbClient -Client $client -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DatabaseName' AND table_name='$core';"
    if ([int]$exists -ne 1) { throw "Kern-Tabelle '$core' fehlt nach der Datenbankinitialisierung." }
  }
  $count = Invoke-DbClient -Client $client -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DatabaseName';"
  Write-Info "GAM-Datenbankschema erfolgreich validiert ($count Tabellen, alle Kern-Tabellen vorhanden)."
} catch {
  Write-Err "Die GAM-Datenbank konnte nicht vorbereitet werden: $($_.Exception.Message)"
  exit 1
}
