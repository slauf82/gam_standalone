# Schritt 40d4 – Marketing 403-Fix und mobile Scan-Seite

## Behoben
- lokale Netzwerkadressen (192.168.x.x, 10.x.x.x und private 172.16–31.x.x) sind für CORS freigegeben
- Beep-, Scan-, Status- und Historienaufrufe funktionieren damit auch bei Aufruf über die lokale PC-IP
- mobile Scan-Ansicht besitzt eine eigene Auswahl der Marketingaktion
- beim ersten Öffnen wird in der mobilen Ansicht die erste verfügbare Aktion vorausgewählt
- Kameraauswahl bei mehreren Webcams
- BarcodeDetector-Live-Erkennung über localhost
- manuelle Barcodeeingabe bleibt immer als Fallback verfügbar

## Test
1. Desktop über `http://localhost:<frontend-port>` öffnen, Marketingaktion auswählen und Webcam starten.
2. Barcode scannen oder manuell eingeben und mit Enter bzw. `Beep +1` buchen.
3. Smartphone über `http://<PC-IP>:<frontend-port>` öffnen und `Smartphone-/Scan-Ansicht öffnen` verwenden.
4. Dort eine Aktion aus der Auswahlliste wählen und manuelle Eingabe/Workflow-Schritte testen.
5. Die Smartphone-Livekamera bleibt über HTTP browserbedingt gesperrt; die Seite selbst und die manuelle Erfassung funktionieren.
