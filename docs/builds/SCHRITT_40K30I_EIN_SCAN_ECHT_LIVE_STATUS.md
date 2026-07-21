# Schritt 40k30i – genau ein Scan, sofort live

## Korrekturen

- Der Session-Start legt nur noch eine Discovery-Session an.
- Die eigentliche Suche beginnt erst, nachdem der Browser den SSE-Kanal geöffnet hat.
- Pro Session kann der Worker durch einen atomaren Startschutz nur einmal anlaufen.
- Die alten Endpunkte `/discovery/scan` und `/discovery/scan-stream` sind deaktiviert, damit kein versteckter synchroner Vorabscan mehr ausgelöst werden kann.
- Treffer, Fortschritt und Diagnosen werden ab dem ersten Arbeitsschritt über dieselbe SSE-Verbindung live übertragen.
- Der globale Abschluss wird ausschließlich als letztes Ereignis des einzigen Scans gesendet.
- Die Migration der registrierten Geräte prüft vorhandene Spalten über `information_schema`, sodass beim Neustart keine Duplicate-column-Warnungen mehr entstehen.

## Abnahmekriterium

Nach Klick auf „Gerätesuche starten“ beginnt genau ein Suchlauf. Während dieses Suchlaufs bewegen sich Fortschrittsanzeige und Trefferzähler im Frontend. Es gibt keinen unsichtbaren ersten Scan und keinen zweiten Scan nach dessen Abschluss.
