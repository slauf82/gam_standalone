# Schritt 40k32 – Differenzierte Discovery-Auswertung

## Ziel
Discovery-Treffer werden nicht mehr nur als „neu“ oder „bekannt“ dargestellt. Die Auswertung unterscheidet nun nachvollziehbar zwischen neuen Geräten, bestätigten Bestandsgeräten, durch zusätzliche Quellen ergänzten Geräten und möglichen Dublettentreffern.

## Umsetzung
- **Neu erkannt:** noch nicht registrierte Identität.
- **Bestehendes Gerät bestätigt:** erneut erkannt, ohne zusätzliche Fundquelle.
- **Bestehendes Gerät ergänzt:** bereits bekannt und durch eine weitere Discovery-Quelle bzw. ein weiteres Protokoll angereichert.
- **Mögliche Dublette:** getrennte Datensätze mit gleichem Namen, gleicher Seriennummer oder gleicher Hardwareadresse. Diese werden nur kenntlich gemacht und nicht automatisch zusammengeführt.
- Live-Karte, Gesamtstatistik, Statusmeldung und Ergebnisbereiche zeigen die neuen Kategorien.
- SNMP-Treffer bereits bekannter Drucker werden dadurch als „ergänzt“ statt als neue Geräte ausgewiesen, sobald SNMP als zusätzliche Quelle am Datensatz anliegt.

## Sicherheit
Die Änderung betrifft ausschließlich Auswertung und Darstellung. Registrierung, Persistenz, Gerätebestand und vorhandene Quellenkonfigurationen bleiben unverändert.
