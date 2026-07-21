# Schritt 40k18b – Sichtbarer FRITZ!Box-Verbindungstest

## Umgesetzt

- Der Button **„Verbindung testen“** speichert zunächst die aktuell eingetragenen Werte und prüft anschließend genau diese Konfiguration.
- Während des Tests zeigt der Button sichtbar **„Verbindung wird geprüft …“**.
- Erfolgreiche Verbindung mit dauerhaftem grünen Ergebnisbereich.
- Fehlgeschlagene Verbindung mit dauerhaftem roten Ergebnisbereich und konkretem Prüfhinweis.
- Erfolgsmeldung enthält, soweit von TR-064 geliefert:
  - FRITZ!Box-Modell
  - FRITZ!OS-Version
  - Host/Adresse
  - Anzahl aller geladenen Geräte
  - Anzahl aktuell aktiver Geräte
  - Vorschau der ersten zehn Gerätenamen
- Fehler werden in Konfigurations-, Authentifizierungs- und Erreichbarkeitsprobleme unterschieden.
- Zusätzlich erscheint ein GAM-Toast für Erfolg oder Fehler.
- Reine Kennwort-Anmeldung ohne Benutzername und Anmeldung mit FRITZ!Box-Benutzer bleiben unterstützt.

## Technische Hinweise

Der Test verwendet die TR-064-Dienste `DeviceInfo:1` und `Hosts:1`. Kann die FRITZ!Box keine Modell- oder Versionsdaten liefern, bleibt der eigentliche Gerätetest trotzdem nutzbar.

Ein vollständiger Maven-Build war in der isolierten Erstellungsumgebung nicht möglich, weil Maven nicht installiert war und die Maven-Distribution ohne Netzwerkzugriff nicht nachgeladen werden konnte.
