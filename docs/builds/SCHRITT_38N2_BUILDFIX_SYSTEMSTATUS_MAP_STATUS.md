# Schritt 38n2 – Buildfix SystemStatusController

## Ziel
38n kompilierte nicht, weil `Map.of(...)` in `SystemStatusController` mit mehr als 10 Key/Value-Paaren verwendet wurde.

## Änderung
- `Map.of(...)` für die Modulübersicht durch `LinkedHashMap` + `modules.put(...)` ersetzt.
- Reihenfolge der Module bleibt erhalten.
- Keine fachlichen Änderungen an 38n.
- Arbeitsplatzausstattung bleibt unverändert enthalten.

## Ergebnis
Der Java-Buildfehler in `SystemStatusController.java` ist behoben.
