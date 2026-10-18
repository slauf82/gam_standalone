# 40k36t80 – Android-App-Blöcke nur auf echten Android-Geräten

## Ursache
Die bisherige UI-Prüfung suchte nach `android`/`adb` im kompletten Discovery-Protokoll.
Dadurch konnte ein Windows-PC allein wegen installierter Software wie
`Android SDK Platform-Tools` als Android-App-Gerät erkannt werden.

## Fix
- Android-App-Gruppen werden primär anhand der ermittelten Identitätsplattform angezeigt.
- Ist die Plattform noch unbekannt, gelten nur Geräteklassifikation oder eine explizite ADB-Quelle als Fallback.
- Der komplette Software-/Discovery-Protokolltext wird nicht mehr für die Plattformentscheidung verwendet.
- Dasselbe gilt für die Android-Auswertung in Reports.

## Wirkung
Auf Windows-, Linux- und macOS-Geräten verschwinden die irreführenden Bereiche
`Benutzer-Apps (0)` und `System-Apps (0)`, sofern das Gerät nicht tatsächlich Android ist.

Die funktionierende Windows-Registry-/Win32-Softwareinventarisierung bleibt unverändert.
