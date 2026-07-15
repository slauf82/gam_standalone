# Schritt 40a16 – Einstellungen: Module und Workflow-Gruppierung

- Der zuvor nur dokumentierte Modulschalter aus 40a15 ist jetzt funktional umgesetzt.
- Einstellungen → Module ist sichtbar.
- Module können installationsweit aktiviert/deaktiviert werden.
- Kernmodule Dashboard, Einstellungen, Administration und Benutzer/Rechte bleiben geschützt.
- Einstellungen → Workflows bündelt alle Workflow-Konfigurationen.
- Rechnungsworkflow ist der erste Unterpunkt; weitere Workflows sind vorbereitet.
- Deaktivierte Module werden in Login-Auswahl und Navigation ausgeblendet.
- Daten werden beim Deaktivieren nicht gelöscht.

## Test
1. Einstellungen öffnen: Allgemein, Module und Workflows müssen sichtbar sein.
2. Unter Workflows muss Rechnungsworkflow als Unterpunkt erscheinen.
3. Ein optionales Modul deaktivieren und speichern.
4. Neu laden/abmelden: Modul darf nicht mehr in Navigation bzw. Login-Auswahl erscheinen.
5. Modul wieder aktivieren; vorhandene Daten müssen unverändert verfügbar sein.
