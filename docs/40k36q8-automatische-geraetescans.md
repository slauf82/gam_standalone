# 40k36q8 – Automatische Gerätescans

GAM kann Discovery und vollständige Inventarisierung ohne geöffnete Browseroberfläche ausführen. Der Automations-Endpunkt ist absichtlich nur vom Rechner des GAM-Backends (`localhost`) erreichbar.

## Modi

- `DISCOVERY`: führt einen vollständigen Gerätesuchlauf mit den konfigurierten Erkennungsquellen aus.
- `INVENTORY`: inventarisiert alle registrierten Geräte anhand ihrer erkannten Plattform vollständig neu.
- `FULL`: Standard. Inventarisiert zuerst alle registrierten Geräte vollständig und führt anschließend einen kompletten Discovery-Lauf aus. Neu erkannte/registrierte Geräte können über die bestehende Auto-Inventarisierungslogik anschließend ebenfalls verarbeitet werden.

Die Inventarisierungstiefe ist immer **vollständig** (40k36q6).

## Windows Aufgabenplanung

Im Verzeichnis `scripts/automation` liegen:

- `gam-auto-scan.ps1` – eigentlicher Runner
- `gam-auto-scan.cmd` – einfache CMD-Hülle
- `install-windows-task.ps1` – richtet eine tägliche Aufgabe als `SYSTEM` ein
- `remove-windows-task.ps1` – entfernt die Aufgabe wieder

Beispiel (PowerShell als Administrator):

```powershell
.\scripts\automation\install-windows-task.ps1 -Mode FULL -DailyAt 02:00
```

Soforttest:

```powershell
.\scripts\automation\gam-auto-scan.ps1 -Mode FULL
```

## Linux/macOS Scheduler

`scripts/automation/gam-auto-scan.sh FULL` kann über cron, systemd timer oder launchd gestartet werden.

## Backend-Endpunkte

- `POST /api/inventory/automation/run` mit `{ "mode": "FULL" }`
- `GET /api/inventory/automation/status`
- `GET /api/inventory/automation/history?limit=20`

Alle drei Endpunkte akzeptieren ausschließlich Loopback-Zugriffe. Externe Rechner erhalten HTTP 403.

## Laufprotokoll

Jeder Lauf wird in `gam_inventory_automation_run` mit Modus, Status, Start/Ende, Dauer, Anzahl gefundener/registrierter Geräte sowie erfolgreichen und fehlgeschlagenen Inventarisierungen protokolliert.

## Rechtehinweis

Gespeicherte SSH-/WinRM-/ADB-Zugänge werden wie bei manuellen Inventarisierungen benutzt. Ein transient eingegebenes macOS-sudo-Kennwort wird bewusst nicht gespeichert und steht einem unbeaufsichtigten Lauf deshalb nicht zur Verfügung. Soll ein Mac auch automatisiert privilegierte Daten liefern, muss der verwendete technische Zugang die dafür erforderlichen Rechte ohne interaktive Kennworteingabe besitzen; dies sollte nur bewusst und nach dem lokalen Sicherheitskonzept eingerichtet werden.
