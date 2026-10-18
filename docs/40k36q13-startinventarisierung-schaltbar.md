# 40k36q13 – Startinventarisierung schaltbar

- Neue persistente Geräteverwaltungs-Einstellung:
  `Bestehende Geräte beim GAM-Start automatisch vollständig inventarisieren`
- Standardwert: aktiviert.
- Einstellung wird in `gam_inventory_startup_settings` gespeichert.
- Bei aktiviertem Zustand startet die Inventarisierung nach `ApplicationReadyEvent`.
- Bei deaktiviertem Zustand wird beim Backend-Start keine Startinventarisierung ausgelöst.
- Manuelle Inventarisierungen, Gerätesuche/Discovery, Betriebssystem-Verbindungstests und Scheduler-Läufe bleiben davon unabhängig.
- Die Änderung gilt ab dem nächsten Backend-Start; ein bereits laufender Startlauf wird nicht rückwirkend abgebrochen.
- Der globale Inventarisierungsstatus aus 40k36q10 erkennt den Startlauf über `triggeredBy = automatisch (Startinventarisierung)`.
