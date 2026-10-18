# 40k36t51 – Windows-Konsolidierung

- Normaler WinRM-Plattformlauf verwendet jetzt `recordVerifiedInventoryHit`, nicht mehr `recordDiscoveryHit`.
- Der Remote-Mapper führt dieselben sichtbaren Windows-Detailbezeichnungen wie backend-lokal:
  Betriebssystem, Hardware, RAM, Storage, Netzwerk, Software, Security, Updates und Berechtigungen.
- Remote-Gerätename verwendet korrekt `COMPUTER_NAME`; Hersteller/Modell werden gemeinsam übernommen.
- UAC-Handoff wurde gehärtet:
  - `Start-Process -Verb RunAs` ohne stdout/stderr-Redirect;
  - der erhöhte Wrapper schreibt selbst in temporäre Ergebnis-/Statusdateien;
  - GAM liest erst nach Prozessende die Dateien;
  - UAC-Abbruch, Timeout und fehlende Ergebnisdatei werden separat behandelt.
- Ein lokaler UAC-Lauf gilt nur dann als „erhöht erfolgreich“, wenn das Inventar selbst `Administratorrechte: Ja` meldet.
- SYSTEM bleibt eine getrennte dritte Inventarisierungsstufe.
