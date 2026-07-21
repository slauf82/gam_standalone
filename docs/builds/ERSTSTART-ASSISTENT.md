# GAM 2.1.0 Preview 2 – grafischer Erststart-Assistent

## Ablauf

1. Das Backend prüft JDK 21 und MariaDB und stellt nur die technische Erreichbarkeit sicher.
2. Es werden vor dem Assistenten keine GAM-Fachdaten und keine Demodaten importiert.
3. Das Frontend erkennt über `/api/setup/status`, ob die Einrichtung erforderlich ist.
4. Der Nutzer wählt zuerst seine Sprache.
5. Administrator, Praxisdaten und Datenbankvariante werden im Frontend erfasst.
6. `/api/setup/initialize` bereinigt eine noch nicht eingerichtete Ziel-Datenbank und importiert die gewählte Preview-2-Vorlage vollständig.
7. Nach Abschluss wird der normale Login in der gewählten Sprache angezeigt.

Eine bereits eingerichtete Datenbank wird durch den Assistenten nicht verändert.
