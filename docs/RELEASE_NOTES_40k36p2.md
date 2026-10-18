# GAM 2.1.0 – Schritt 40k36p2

## Erkennungsquellen 2.0

Die in 40k36p1 ausgelagerten Betriebssystem- und Mobilgerätequellen verwenden nun ein gemeinsames Bedienkonzept.

- Einheitliche Karten für Windows lokal, Windows WinRM, Linux lokal, Linux SSH, macOS lokal, macOS SSH und Android USB/WLAN
- Einheitliche Aktionen: Aktivieren, Konfigurieren, Verbindung testen und Diagnose
- Klarer Status je Quelle und Hinweis auf die Verfügbarkeit des benötigten Adapters
- Wählbare Inventarisierungstiefe: Basis, Standard oder vollständig
- Inventarisierungstiefe wird lokal je Browserarbeitsplatz gespeichert
- WinRM führt weiterhin zur vollständigen vorhandenen Konfiguration und zum echten Host-Verbindungstest
- Gerätebezogene SSH- und ADB-Zugänge bleiben bewusst am konkreten Gerät und nicht im Inventarisierungsdialog
