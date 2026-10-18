# Schritt 40k37a – Apple-Mobile-Basis (iOS/iPadOS)

Ausgangspunkt: `40k36t126a` / `v2.1.0-preview5`.

## Ziel

Erster real testbarer Unterbau für normale, nicht jailbroken iPhones und iPads. 40k37a erfindet keine Daten und meldet fehlende Voraussetzungen ausdrücklich.

## Implementiert

- getrennte Discovery-Schalter `IOS_LOCAL` und `IPADOS_LOCAL` in der persistenten GAM-Konfiguration
- nativer Adapter über `libimobiledevice`: `idevice_id`, `ideviceinfo`, optional `idevicepair`
- USB-/usbmuxd-/lockdown-Erkennung mit UDID
- Trust-/Pairing-Prüfung
- Basisdaten: Gerätename, ProductType, ProductVersion, BuildVersion, Seriennummer, Modellnummer, Hardwaremodell, CPU-Architektur, WiFi-/Bluetooth-Adresse und Zeitzone, soweit das Gerät diese über Lockdown liefert
- korrekte Klassifikation iPhone → iOS / Smartphone und iPad → iPadOS / Tablet
- Discovery-Diagnosephase `APPLE_MOBILE`
- API-Status und echter Verbindungstest
- Gerätemanager zeigt Verbindungstyp sowie erkannte/gepairte Geräte
- Windows-Fallback erkennt physisch sichtbare Apple-USB-Hardware als Hinweis, meldet aber ohne native Werkzeuge bewusst **keinen** erfolgreichen Pairing-/Inventarisierungsstatus

## Bewusste Grenze von 40k37a

Noch keine tiefe Plattforminventarisierung, keine App-Inventarisierung, kein MDM/Supervision und kein Jailbreak-Pfad. Diese Funktionen bauen erst nach dem realen 37a-Test auf dem nachgewiesenen Pairing-/Lockdown-Unterbau auf.

## Erster Realtest

1. iPad per USB mit dem GAM-Host verbinden und auf dem iPad ggf. „Diesem Computer vertrauen“ bestätigen.
2. Im Gerätemanager `iPadOS lokal` aktivieren.
3. `Apple-Gerät prüfen` ausführen.
4. Erwartet bei vollständiger Toolchain: `libimobiledevice / usbmuxd / lockdown`, mindestens 1 erkanntes und 1 gepairtes Gerät.
5. Danach Gerätesuche starten; das iPad soll als `Tablet / iPadOS` mit echten Basisdaten erscheinen.

Wenn `TOOLS_MISSING` erscheint, ist die Hardwareerkennung ggf. bereits sichtbar, aber die libimobiledevice-Werkzeuge fehlen auf dem GAM-Host. Dieser Zustand ist absichtlich kein grüner Erfolg.
