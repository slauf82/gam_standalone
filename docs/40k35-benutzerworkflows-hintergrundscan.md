# Schritt 40k35 – Benutzeraufgaben statt Technik + Hintergrundscan

## Ziel
GAM ordnet Gerätefunktionen künftig nach den Aufgaben der Benutzer und nicht nach den darunterliegenden Protokollen.

Gemeinsamer Ablauf:

1. Gerät finden
2. Gerät verbinden
3. Gerät inventarisieren
4. Gerät verwalten

Technische Zuordnung im Hintergrund:

- Android: ADB
- Linux: SSH
- Windows: WinRM / PowerShell

## Oberflächenänderungen

- Android-Inventarisierung heißt nun „Android-Gerät verwalten“.
- ADB-Aktionen werden als Verbindung prüfen, Gerät koppeln, Gerät verbinden und Verbindung trennen angeboten.
- Windows-Konfiguration heißt „Windows-Geräte verbinden“; WinRM wird nur noch als technische Umsetzung erklärt.
- Geräteaktionen sind in der gemeinsamen Gruppe „Geräteaufgaben“ zusammengefasst.
- Plattformmenüs verwenden „Android/Windows/Linux verwalten“ statt Protokollnamen als Hauptbegriff.

## Fix: Gerätesuche in inaktiven Browsertabs

Die Live-Abholung wartete nach jedem Ereignis auf `requestAnimationFrame`. Browser halten diesen Mechanismus in inaktiven Tabs an oder drosseln ihn stark. Dadurch lief der Backend-Scan zwar weiter, die Ereignisverarbeitung im Frontend blieb jedoch stehen.

Der Discovery-Client verarbeitet Ereignisse jetzt unabhängig von `requestAnimationFrame`. Der eigentliche Suchlauf bleibt vollständig serverseitig in der Discovery-Session aktiv. Beim Wechsel des Browsertabs werden Ereignisse weiter abgeholt beziehungsweise nach Rückkehr aus dem serverseitigen Ereignispuffer nachgezogen.

## Technische Grenzen

Browser dürfen Hintergrund-Netzwerkaktivität weiterhin drosseln. Das stoppt den serverseitigen Suchlauf jedoch nicht mehr. Nach der Rückkehr in den Tab wird der aktuelle Stand aus derselben Discovery-Session nachgeladen.
