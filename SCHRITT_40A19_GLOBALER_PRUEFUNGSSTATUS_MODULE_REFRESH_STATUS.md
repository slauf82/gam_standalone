# Schritt 40a19 – Globaler Prüfungsstatus und sofortige Modulaktualisierung

## Umgesetzt

- Prüfungsstatus wird zentral aus Geräteprüfungen, Inbetriebnahmen und Einweisungen ermittelt.
- Rot bei mindestens einer überfälligen Prüfung.
- Gelb bei mindestens einer innerhalb von 14 Tagen fälligen Prüfung und keiner überfälligen Prüfung.
- Grün ohne aktuellen Handlungsbedarf.
- Status erscheint in der Modulnavigation als farbiger Rahmen und Badge mit Anzahl.
- Dashboard-Rahmen und deutlich sichtbarer Hinweis verwenden dieselbe zentrale Statusberechnung.
- Login-Modulauswahl zeigt den Prüfungsstatus textlich an.
- Speichern der Modulauswahl erzeugt einen Toast oben rechts.
- Aktivierte/deaktivierte Module werden nach dem Speichern ohne Neuanmeldung sofort in Navigation und Dashboard berücksichtigt.

## Test

1. Einstellungen → Module öffnen, ein optionales Modul deaktivieren und speichern.
2. Toast oben rechts prüfen; das Modul muss unmittelbar aus der Navigation verschwinden.
3. Modul wieder aktivieren; es muss unmittelbar erscheinen.
4. Bei überfälligen Prüfungen muss „Prüfungen“ in der Navigation rot markiert sein und die Anzahl zeigen.
5. Dashboard muss denselben roten Status anzeigen.
6. Ohne überfällige, aber mit bald fälligen Prüfungen muss Gelb erscheinen.
7. Ohne Handlungsbedarf muss Grün erscheinen.
