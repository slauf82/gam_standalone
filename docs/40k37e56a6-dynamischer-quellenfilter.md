# 40k37e56a6 – Dynamischer Quellenfilter

- Quellenfilter im Gerätemanager ist jetzt eine Mehrfachauswahl mit Checkboxen.
- Die Liste wird bereits beim Öffnen aus den persistent registrierten Geräten / dem Gerätebestand aufgebaut.
- Neue Discovery-Treffer ergänzen die Liste dynamisch.
- Alle bekannten und neu hinzukommenden Quellen sind standardmäßig aktiviert.
- Nur explizit abgewählte Quellen werden persistent gespeichert; dadurch bleiben neue Quellen rückwärtskompatibel sichtbar.
- „Alle Erkennungsquellen“ schaltet alle aktuell bekannten Quellen gemeinsam an oder aus.
- Mehrere aktivierte Quellen wirken als ODER-Filter.
- Grundlage ist die tatsächliche Discovery-Provenienz, nicht bloß eine erkannte Schnittstelle/Fähigkeit.
- Discovery-, Merge-, Apple-/CoreDevice- und Inventarisierungslogik bleiben unverändert.
