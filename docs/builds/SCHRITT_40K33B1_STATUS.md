# Schritt 40k33b1 – Kategoriesynchronisierung und Demo-Import-Stabilisierung

- Bestehende manuelle Gerätekategorien haben Vorrang.
- Bereits vorhandene automatische Kategorien werden vor neuen ähnlichen Kategorien wiederverwendet.
- Synonyme wie „Linux-PC“, „Computer / Netzwerkadapter“ und „Workstation“ werden einer bestehenden Kategorie „Computer“ zugeordnet.
- Entsprechende Familien existieren auch für Server, Netzwerk, Drucker, Speicher/NAS, Kameras, Mobilgeräte, Robotik, Haushaltsgeräte, Energie und USV.
- Der nachträgliche Beispieldatenimport überspringt eingebettete Transaktions- und Verbindungssteuerbefehle.
- Rollback wird nur bei noch offener Verbindung ausgeführt.
- Der ursprüngliche SQL-Fehler wird mit Anweisungsnummer und gekürzter SQL-Anweisung protokolliert, statt durch „Connection is closed“ verdeckt zu werden.
