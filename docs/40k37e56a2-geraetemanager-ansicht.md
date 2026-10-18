# 40k37e56a2 – Gerätemanager: Einstellungen und Ergebnisansicht

- Baut auf 40k37e56a1 auf.
- Quelleneinstellungen werden über `⚙ Einstellungen` in einem separaten Dialog geöffnet.
- Die Hauptfläche des Gerätemanagers konzentriert sich auf Such- und Geräteergebnisse.
- `✓ Bereiche` steuert die Sichtbarkeit der vorhandenen Ergebnisbereiche:
  - Bestehende Geräte ergänzt
  - Bestehende Geräte bestätigt
  - Mögliche Dublettentreffer
  - Neu erkannte Geräte
  - Ignoriert
  - Registriert / Inventarisieren
  - Gerätebestand
- Die Bereichsauswahl wird lokal persistent gespeichert und verändert ausschließlich die Anzeige.
- Der Gerätebestand ist nun selbst ein auf-/zuklappbarer Hauptbereich.
- Ein zusätzlicher Quellenfilter filtert die angezeigten Suchergebnisse nach tatsächlicher Erkennungsquelle, ohne Discovery oder gespeicherte Daten zu verändern.
- Apple-/CoreDevice-, Merge- und Backend-Logik aus e56a1 bleiben unverändert.
