# 40k36q29 – Fortschrittsanzeige Beispieldatenimport

- Echter Backend-Fortschritt statt Animation.
- Neuer Status-Endpunkt `/api/database/demo/progress`.
- Phasen: START, RUNTIME_SCHEMA, BEISPIELDATEN, ABSCHLUSS, FERTIG/FEHLER.
- Prozentwert basiert auf tatsächlich verarbeiteten SQL-Anweisungen.
- Frontend pollt während des Imports alle 500 ms.
- Anzeige von Prozent, aktuellem Schritt sowie verarbeiteten/gesamten Arbeitsschritten.
- Fehlerstatus bleibt mit letzter Fortschrittsposition sichtbar.
