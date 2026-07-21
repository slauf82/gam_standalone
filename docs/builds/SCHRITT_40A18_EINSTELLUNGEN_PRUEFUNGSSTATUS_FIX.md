# Schritt 40a18 – Einstellungen- und Prüfungsstatus-Fix

## Behoben

- Laufzeitfehler `loadNotificationSettings is not defined` im Einstellungsmodul behoben.
- Auch der zugehörige Speicheraufruf wird wieder auf die vorhandene Meldungseinstellungs-Implementierung abgebildet.
- Prüfungsbereiche erhalten einen sofort sichtbaren Statusrahmen:
  - Rot: mindestens eine Prüfung überfällig
  - Gelb: keine überfällige, aber mindestens eine Prüfung innerhalb der nächsten 14 Tage
  - Grün: aktuell kein unmittelbarer Prüfungsbedarf

## Test

1. Einstellungen öffnen; die Seite muss ohne Fehlermeldung erscheinen.
2. Meldungszeiten speichern und Seite neu laden.
3. Prüfungen öffnen. Bei überfälligen Prüfungen muss der betreffende Bereich sofort rot umrandet sein.
4. Bereiche mit nur bald fälligen Prüfungen müssen gelb, unkritische Bereiche grün erscheinen.
