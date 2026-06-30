# Schritt 38g5 – Bestellworkflow und E-Mail-Auslösung

## Ziel

Das Bestelltool löst beim Auslösen einer Bestellung nicht nur einen lokalen Bestellentwurf aus, sondern startet auch den vorbereiteten Workflow und öffnet die vorbereitete Bestell-E-Mail mit definierter E-Mail-Adresse, Betreff und Mailtext.

## Umgesetzt

- Bestellentwürfe besitzen nun Betreff und Mailtext.
- Aus Verbrauchsmaterial erzeugte Bestellentwürfe übernehmen die Hersteller-/Bestell-E-Mail-Adresse.
- Standard-Betreff wird automatisch aus dem Bestellinhalt erzeugt.
- Standard-Mailtext wird automatisch aus den Positionen erzeugt.
- Button „Bestellung auslösen“ im Dialog und in der Entwurfs-Tabelle.
- Beim Auslösen wird eine Workflow-Aufgabe angelegt.
- Der Bestellentwurf wird auf Status „bestellt“ gesetzt.
- Workflow-Aufgaben-ID und Auslösezeitpunkt werden am Entwurf gespeichert.
- Die Bestell-E-Mail wird per mailto mit Empfänger, Betreff und Mailtext geöffnet.
- Toast-/Meldungsverlauf enthält Änderungsdetails inklusive Workflow-Aufgabe, Betreff und Mailtext.

## Technischer Hinweis

Der E-Mail-Versand erfolgt in diesem Schritt bewusst frontendseitig über den lokalen Mailclient (`mailto:`). Damit bleibt der Schritt ohne SMTP-/Backend-Konfiguration testbar. Eine serverseitige SMTP-Anbindung kann später ergänzt werden.

## Build

Frontend-Build erfolgreich getestet.

## Keine Änderungen

- Keine Datenbankänderungen
- Keine Backendänderungen
- Keine SMTP-Konfiguration
