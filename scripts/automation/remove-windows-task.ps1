param([string]$TaskName = 'GAM 2.0 automatische Geräteinventarisierung')
& schtasks.exe /Delete /TN $TaskName /F
exit $LASTEXITCODE
