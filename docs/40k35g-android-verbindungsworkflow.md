# Schritt 40k35g – GAM-eigener Android-Verbindungsworkflow

## Ziel
Android-Geräte können direkt aus dem Bereich „Registriert“ gekoppelt, verbunden und erneut inventarisiert werden. Der Workflow bleibt auch sichtbar, wenn ein Android-Gerät zuvor fälschlich als Linux oder unbekannt klassifiziert wurde.

## Änderungen
- Aktionsmenü „Als Android-Gerät verbinden“ für jede registrierte Geräteidentität als Wiederherstellungsweg.
- Android-Verbindungs- und Inventarisierungsbereich im Identitätsdialog auch bei noch falscher Plattformzuordnung verfügbar.
- IP-Adresse wird aus der registrierten Geräteadresse vorbelegt.
- Nach dem Pairing wird `adb connect` mit der ausgewählten Geräteidentität ausgeführt; Host und Port werden dauerhaft für die Wiederverbindung gespeichert.
- Vor Android- und App-Inventarisierung versucht GAM automatisch eine Wiederverbindung zum bekannten ADB-Ziel.
- Pairing-Codes werden weiterhin nicht gespeichert.
- ADB-Evidenz erhält bei der Plattformbestimmung Vorrang vor alten Linux-Protokollresten. Eine erfolgreiche ADB-Inventarisierung korrigiert damit die Anzeige auf Android.

## Bedienung für das Fossibot F107 Pro
1. Im Bereich „Registriert“ das Aktionsmenü des F107 Pro öffnen.
2. „Als Android-Gerät verbinden“ wählen.
3. Bei noch gültiger Kopplung „Gerät verbinden“ verwenden und den aktuellen ADB-Verbindungsport eintragen.
4. Falls Android die Kopplung nicht mehr kennt: „Gerät koppeln“, Pairing-Port und Code eintragen, danach den separaten ADB-Verbindungsport verwenden.
5. „Inventarisierung starten“ bzw. „Inventarisierung erneut ausführen“ anklicken.

Hinweis: Beim modernen drahtlosen Debugging können Pairing-Port und ADB-Verbindungsport verschieden sein und sich nach dem erneuten Aktivieren ändern.
