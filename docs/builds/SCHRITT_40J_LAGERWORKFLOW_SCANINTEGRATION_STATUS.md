# Schritt 40j – Lagerworkflow 2.0 mit Scanintegration

- Die zentrale Scan-Engine läuft ausschließlich im Hintergrund und ist kein auswählbares Modul mehr.
- Die Lagerverwaltung fordert Scans mit dem Kontext `warehouse` an.
- Unterstützte Aktionen: Wareneingang, Entnahme/Verbrauch, Rückgabe, Umlagerung und Inventur.
- Kamera, USB-Barcodescanner und manuelle Eingabe verwenden dieselbe Scan-Komponente wie Marketing.
- Die Scan-Engine liefert nur den normalisierten Code; die fachliche Buchung bleibt im Lagerworkflow.
- Die letzten Lager-Scans werden während der Sitzung nachvollziehbar angezeigt.
