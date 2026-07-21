# Schritt 40k20 – Home-Assistant-Identitätsquellen

## Umgesetzt
- Beliebig viele Home-Assistant-Instanzen als parallele Identitätsquellen.
- Konfiguration mit Bezeichnung, Standort, URL, Aktivierung und Long-Lived Access Token.
- Speichern, Bearbeiten, Löschen und sichtbarer Verbindungstest.
- Verbindungstest zeigt Home-Assistant-Version, Geräteanzahl und eine Vorschau mit Gerätetyp.
- Discovery lädt aktive Home-Assistant-Quellen über die REST-API und führt sie mit FRITZ!Box-, MAC-, IP-, ARP- und SSDP-Treffern zusammen.
- Zusammenführung bevorzugt MAC-Adresse, danach quellenbewusste IP-Adresse.
- Geräte werden anhand von Entity-Domain, Geräteklasse, Name, Hersteller und Modell klassifiziert.
- Unterstützte konkrete Gruppen umfassen Computer, Drucker, Kameras, Robotik, Audio/Receiver, Klima/Gebäudetechnik, Haushaltsgeräte, Energie/Wechselrichter, NAS/Speicher und Netzwerkgeräte.
- `Home Assistant Gerät` bzw. `AVM / FRITZ!Box Gerät` wird nur als Fallback verwendet, wenn keine konkretere Zuordnung möglich ist.
- Discovery-Diagnose enthält eine eigene Phase `HOME_ASSISTANT`.

## Build-Hinweis
Ein vollständiger Build war in der isolierten Umgebung nicht möglich, weil Maven lokal fehlte und ohne Netzwerkzugriff nicht nachgeladen werden konnte. Frontend-Abhängigkeiten waren im bereinigten Quellpaket ebenfalls nicht enthalten.
