# Schritt 31i – Login UI vollständig an bestehende i18n angebunden

## Ziel

Kein neues Backend-System, kein LibreTranslate-Experiment.
Stattdessen werden die noch festen Login-/Modultexte an die bereits funktionierende Frontend-i18n aus Schritt 31e angebunden.

## Gefixt

- verbleibende feste Logintexte durch `gamUi("...")` ersetzt
- Modulnamen werden über `gamModuleLabel(...)` angezeigt
- Modulnamen bleiben intern unverändert, nur die Anzeige wird übersetzt
- Dictionary um fehlende Keys ergänzt
- kein neuer YAML-Block
- kein experimenteller Backend-Translation-Cache

## Erwartung

Beim Wechsel der Oberflächensprache sollen jetzt auch diese Texte umschalten:

- Kompatibler Login über bestehende accounts-Tabelle.
- Anwendung wählen
- Lesemodus-Hinweis
- Rechnungsprogramm
- Geräteverzeichnis
- Lagerverwaltung
- Kassenbuch
- Aufgabenverwaltung
- Freigabemanagement
- Bestelltool
- Personaldaten
- Arbeitsplatzausstattung
- Preisliste
- Reports
- Administration
- 2FA-/Passkey-Texte

## Wichtig

Die PDF-Sprache im Rechnungsprogramm bleibt separat.
