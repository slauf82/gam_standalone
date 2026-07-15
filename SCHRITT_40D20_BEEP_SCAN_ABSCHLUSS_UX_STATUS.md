# Schritt 40d20 – Beep, Scanabschluss und Marketing-UX

## Umgesetzt
- Sofortiger Bestätigungston bei erfolgreicher nativer oder ZXing-Barcodeerkennung.
- Ton wird bereits beim Erkennen ausgegeben, damit der Artikel sofort von der Kamera entfernt werden kann.
- Einstellbarer Bestätigungston und Fehlerton im Marketingworkflow.
- Vorschau-Schaltflächen zum Anhören beider Töne.
- Einstellbare Lautstärke, Dauer sowie Erfolgs- und Fehlerfrequenz.
- Einstellungen werden persistent in `gam_settings` gespeichert.
- Fehlerton bei fehlgeschlagenen Mengenbuchungen und Scan-Rücknahmen.
- Abgeschlossene Marketingaktionen werden standardmäßig aus Liste und Auswahl ausgeblendet.
- Option „Abgeschlossene Aktionen anzeigen“ für Historie und Nachkontrolle.
- Nach „Abschließen“ wird die aktive Aktion abgewählt und die Scanbedienung zurückgesetzt.
- Kamera, Livebild und Aktionsauswahl bleiben für die nächste Aktion verfügbar.
- Auf abgeschlossene Aktionen können keine weiteren Scanbuchungen über die normale Auswahl erfolgen.

## Standardwerte
- Bestätigungston: aktiv
- Fehlerton: aktiv
- Lautstärke: 55 %
- Dauer: 90 ms
- Erfolgsfrequenz: 1000 Hz
- Fehlerfrequenz: 440 Hz

## Prüfung
- Direkter Vite-Produktionsbuild erfolgreich.
- Release-Paket ohne `_work`, `node_modules`, Beispieldatenbank, Logs und temporäre Buildverzeichnisse.
