# Schritt 40k35b – Android-Appanzeige

## Änderung

Die bei der Android-Inventarisierung gespeicherten Apps werden im Geräteidentitätsdialog jetzt automatisch geladen und direkt sichtbar angezeigt.

- Bereich „Installierte Apps“ ist standardmäßig geöffnet.
- App-Daten werden beim Öffnen des Android-Geräts sofort geladen.
- Benutzer- und System-App-Anzahl stehen direkt in der Überschrift.
- Während des Ladens erscheint ein klarer Ladehinweis.
- Falls keine Datensätze geliefert werden, weist GAM gezielt auf eine erneute App-Inventarisierung hin.

## Technischer Hintergrund

Die App-Inventarisierung selbst war erfolgreich und persistierte die Pakete bereits. Die Anzeige war jedoch nochmals in einem geschlossenen Lazy-Loading-Unterbereich verborgen und wurde erst nach manuellem Öffnen abgefragt.
