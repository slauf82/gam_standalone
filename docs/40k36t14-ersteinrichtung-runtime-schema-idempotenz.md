# GAM 2.1.0 – Schritt 40k36t14

## Erststart: Runtime-Schema idempotent ausführen

Ausgangspunkt ist 40k36t13.

Beim Erststart auf CachyOS mit MariaDB konnte sowohl die leere als auch die Beispieldatenbank mit
`Multiple primary key defined` (MariaDB 1068) abbrechen. Ursache war nicht die Datenbankauswahl,
sondern der gemeinsame Runtime-Schema-Schritt im `FirstRunSetupController`.

Der bereits in 40k36q28 gehärtete Demo-Datenbankpfad toleriert bei der additiven Runtime-Schema-
Ausführung die bekannten Wiederholungsfälle 1060 (duplicate column), 1061 (duplicate key/index)
und 1068 (multiple primary key). Der später hinzugekommene Erststart-Assistent verwendete dagegen
den strikten Basisschema-Ausführer und brach bei 1068 ab.

40k36t14 führt deshalb für das additive Runtime-Schema im Erststart einen eigenen idempotenten
Ausführungspfad ein. Nur MariaDB 1060, 1061 und 1068 werden dort toleriert. Alle anderen SQL-Fehler
bleiben harte Fehler und werden mit Anweisungsnummer und MariaDB-Fehlercode gemeldet.

Unverändert bleiben:
- Basisschema-Import (weiterhin strikt)
- Beispieldatenimport (weiterhin strikt)
- Linux-Inventarisierung Backend-lokal und SSH
- Softwarequellen-/Detailtabellen-UI aus 40k36t13

## Validierung

Die Änderung wurde strukturell gegen den bestehenden 40k36q28-Pfad abgeglichen. Ein vollständiger
Maven-Compile konnte in der Build-Umgebung nicht ausgeführt werden, weil weder Maven vorinstalliert
ist noch der Maven-Wrapper ohne Netzwerkzugriff seine Maven-Distribution herunterladen kann.
