# Schritt 40k31k – Gerätename und Geräteart direkt bearbeiten

Aufbauend auf 40k31j wurden Reportbereich und Gerätemanager vereinheitlicht.

## Neu
- Geräteart steht im Report-Auswahlbaum direkt am Anfang jeder Gerätezeile.
- Report-Auswahl verwendet dieselbe geordnete Kategorienliste wie der Gerätemanager.
- Geräteart kann im Reportbereich geändert werden; das Gerät wird danach sofort neu einsortiert.
- Gerätename kann im Reportbereich direkt geändert werden (Speichern bei Enter oder Fokusverlust).
- Gerätename registrierter Geräte kann im Gerätemanager direkt geändert werden.
- Name und Geräteart gespeicherter Inventargeräte können im Gerätemanager direkt geändert werden.
- Für registrierte Geräte wurde ein eigener Backend-Endpunkt zur persistenten Namensänderung ergänzt.

## Hinweis
Neu entdeckte, noch nicht gespeicherte Geräte bleiben bis zur Übernahme schreibgeschützt, da sie noch keinen persistenten Inventardatensatz besitzen.
