# 40k36t62 – Windows SYSTEM-Inventarisierung

- Backend-lokal: Button „Mit SYSTEM-Rechten neu inventarisieren“ verwendet jetzt eine echte einmalige SYSTEM-Ausführung.
- Eine UAC-Zustimmung erlaubt GAM ausschließlich, eine zufällig benannte temporäre geplante Aufgabe `GAM-Inventory-<UUID>` anzulegen und zu starten.
- Die Aufgabe läuft als `NT AUTHORITY\SYSTEM`, `ServiceAccount`, `Highest`.
- Der vorhandene vollständige 9-Modul-Collector wird wiederverwendet.
- Im Wrapper wird `NT AUTHORITY\SYSTEM` vor dem Collector explizit geprüft.
- Ergebnisrückgabe erfolgt über die bestehende GAMKV-Dateikette.
- Nach dem Lauf wird die Aufgabe wieder deregistriert; Temp-Dateien werden entfernt.
- Kein Dienst und keine dauerhafte SYSTEM-Komponente werden installiert.
- TrustedInstaller wird nicht verwendet.
- BitLocker läuft im SYSTEM-Kontext erneut durch denselben Get-BitLockerVolume/manage-bde-Pfad, sodass der Vergleich Admin ↔ SYSTEM möglich ist.
