# Schritt 40b4 – Zahlungsworkflow, Mahnwesen und Patientenportal

## Umgesetzt

- Zahlungserinnerung direkt an der geöffneten Rechnung erzeugen
- Mahnung 1, Mahnung 2 und Mahnung 3 erzeugen
- Inkasso-/Rechtsanwaltshinweis als letzte Eskalationsstufe
- Status und Historie des Zahlungsworkflows werden automatisch fortgeschrieben
- Dokumente werden dauerhaft in `payment_workflow_document` gespeichert
- Dokumentliste direkt im Zahlungsworkflow
- PDF-Aufruf direkt an der Rechnung
- Bereitstellung derselben Dokumente im bestehenden Patientenportal
- Vorlesefunktion des Patientenportals berücksichtigt auch Zahlungserinnerungen und Mahnungen
- gemeinsame OpenHTMLToPDF-Basis mit PDF/A-3-U und aktiviertem PDF/UA-Tagging
- Gesellschaft, Empfänger, Rechnungsnummer, offener Betrag und Fälligkeit werden übernommen

## Barrierefreiheit

Die Dokumente verwenden dieselbe technische Grundstrategie wie die Rechnungs-PDFs: OpenHTMLToPDF, PDF/A-3-U, aktiviertes PDF/UA-Tagging, semantische HTML-Struktur und Portal-Vorlesbarkeit. Eine verbindliche PAC-/PDF/UA-Konformitätsaussage setzt weiterhin die Prüfung der konkret erzeugten PDF-Datei mit PAC voraus.

## Test

1. Bestehende unbezahlte Rechnung öffnen.
2. Im Zahlungsworkflow „Zahlungserinnerung als PDF“ wählen.
3. Prüfen, ob Status und Historie aktualisiert werden.
4. Das neue Dokument über „Portal-Dokumente“ öffnen.
5. Patientenportal der Rechnung öffnen und prüfen, ob das Dokument dort erscheint.
6. Portal-Vorlesefunktion für das Dokument testen.
7. Mahnung 1–3 und Inkasso analog testen.
8. Eine erzeugte PDF zusätzlich mit PAC prüfen.
