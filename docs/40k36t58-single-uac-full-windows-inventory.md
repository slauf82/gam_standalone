# 40k36t58 – Eine UAC-Abfrage für den kompletten Windows-Lauf

- Erhöhte backend-lokale Windows-Inventarisierung startet jetzt genau einen UAC-Prozess.
- In diesem Prozess laufen alle neun Module nacheinander:
  SYSTEM, BIOS_BOARD, CPU_MEMORY, STORAGE, NETWORK, SOFTWARE, SECURITY, MAINTENANCE, UPDATES.
- Der UAC-Gesamtlauf hat bis zu 300 Sekunden Zeit; die bisherigen kurzen Modul-Timeouts erzeugen keine wiederholten UAC-Abfragen mehr.
- Jedes Modul schreibt einen eigenen `MODULE_<NAME>`-Status in den GAMKV-Strom, damit GAM erfolgreiche/fehlgeschlagene Teilmodule weiterhin sichtbar diagnostizieren kann.
- Normale nicht erhöhte lokale Inventarisierung bleibt weiterhin modular mit den bisherigen Einzel-Timeouts.
- WinRM bleibt unverändert.
