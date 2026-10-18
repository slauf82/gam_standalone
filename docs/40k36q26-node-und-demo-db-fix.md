# 40k36q26 – Node/npm und Demo-Datenbankpfad

- start_frontend.sh installiert Node.js/npm automatisch (macOS Homebrew; Linux apt/dnf/yum/pacman).
- Ersteinrichtung verwendet für leer und Demo denselben Strukturpfad: leeres Basisschema -> aktuelles Runtime-Schema -> optional Beispieldaten -> Administrator.
- Der Erstassistent verwendet nicht mehr den separaten alten Voll-Demo-Dump.
- ensureRuntimeSchema() importiert die Runtime-SQL-Datei jetzt tatsächlich.
- Runtime-Schema um aktuelle technische Repository-Tabellen ergänzt, darunter gam_discovery_platform_inventory_runs.
- Späterer Beispieldatenimport zieht vor dem Datenimport das Runtime-Schema additiv nach.
- Erstassistent und späterer Import verwenden dieselbe Beispieldaten-Konvertierungsdatei.
