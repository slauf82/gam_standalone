# 40k36r4 – SMART/smartmontools-Fallbacks

- smartmontools-Paketstatus wird über Homebrew diagnostisch erkannt.
- smartctl wird unabhängig vom Paketstatus über PATH sowie `/opt/homebrew/bin` und `/usr/local/bin` gesucht.
- Paket installiert, smartctl aber nicht gefunden: Diagnosehinweis, kein Abbruch.
- smartctl gefunden, aber nicht ausführbar: Diagnosehinweis, kein Abbruch.
- smartctl nicht vorhanden: native macOS-SMART-Daten über `diskutil`, kein Abbruch.
- smartctl vorhanden: erweiterte SMART/NVMe-Werte werden verwendet.
- Datenträger/USB-Bridge unterstützt SMART nicht: pro Datenträger als „nicht unterstützt“ markiert, restliche Hardwareanalyse läuft weiter.
- smartctl liefert Fehler oder keine Ausgabe: Status wird pro Datenträger sichtbar, restliche Inventarisierung läuft weiter.
- sudo wird nur non-interaktiv versucht; die reine Inventarisierung fordert kein Kennwort an und installiert nichts.
