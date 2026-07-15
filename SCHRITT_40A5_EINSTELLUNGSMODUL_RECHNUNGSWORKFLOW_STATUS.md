# Schritt 40a5 – Einstellungsmodul und konfigurierbarer Rechnungsworkflow

## Enthalten

- neues zentrales Modul **Einstellungen**
- übersichtliche Tabs für die heutigen und geplanten Workflows
- persistente Datenbanktabelle `gam_settings`
- Prüfungsschritt standardmäßig aktiviert
- Prüfung, Freigabe und Versandstatus einzeln ein-/ausschaltbar
- bestehende laufende Rechnungsworkflows bleiben bedienbar
- Rechnungsanzeige zeigt nur die aktuell konfigurierten Prozessschritte

## Test

1. Einstellungen → Rechnungsworkflow öffnen.
2. Prüfungsschritt aktiviert lassen und Ablauf `Entwurf → Prüfung` prüfen.
3. Prüfungsschritt deaktivieren und speichern.
4. Neue Rechnung öffnen: nächste Aktion muss direkt `Freigeben` sein.
5. Backend neu starten und prüfen, dass die Einstellung erhalten bleibt.
6. Eine bereits im Status `PRUEFUNG` befindliche Rechnung muss weiter freigegeben oder zurückgegeben werden können.
7. Prüfung wieder aktivieren und erneut speichern.

## Standard

`Prüfungsschritt verwenden = aktiviert`
