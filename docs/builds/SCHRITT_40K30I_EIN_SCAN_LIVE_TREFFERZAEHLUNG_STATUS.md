# Schritt 40k30i – Ein Scan, echte Live-Ausgabe und nachvollziehbare Trefferzählung

## Behoben

- Der Discovery-Worker startet erst, nachdem der Browser den SSE-Kanal geöffnet hat.
- Pro Benutzerstart wird genau eine Discovery-Session und genau ein Scan ausgeführt.
- Der SSE-Endpunkt hört nur zu und startet keinen zweiten Suchlauf.
- Die Datenbankerweiterung prüft Spalten vor `ALTER TABLE`; vorhandene Spalten erzeugen keine MariaDB-1060-Warnungen mehr.

## Trefferzählung

Jeder Roh-Treffer einer Erkennungsquelle wird gezählt, auch wenn er mit einem bereits erkannten Gerät zusammengeführt wird. Damit ist nachvollziehbar, warum beispielsweise 69 Treffer nur 48 eindeutige registrierte Geräte ergeben.

Für jedes registrierte Gerät werden dauerhaft gespeichert:

- `last_scan_hits`: Anzahl der Treffer in der letzten Suche
- `detection_count`: kumulierte Trefferzahl über alle Suchen

Die Registriert-Liste zeigt beide Werte. Der Zähler der letzten Suche wird zu Beginn eines neuen Discovery-Laufs zurückgesetzt; die Registrierung und der Gesamtzähler bleiben erhalten.
