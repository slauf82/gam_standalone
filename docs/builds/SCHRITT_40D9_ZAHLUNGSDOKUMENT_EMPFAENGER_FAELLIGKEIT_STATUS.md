# Schritt 40d9 – Zahlungsdokumente: Empfänger und Fälligkeit

## Behoben

- Der sichtbare Platzhalter `b'1'` wird nicht mehr in Zahlungserinnerungen, Mahnungen oder Inkassoschreiben ausgegeben.
- Empfängername und Anschrift werden aus bereinigten Stammdatenfeldern zusammengesetzt.
- Python-/Byte-Literalreste wie `b'1'`, `b"1"`, `b'true'` oder `b'false'` werden als ungültige technische Platzhalter verworfen.
- Andere Byte-Literale werden vor der Ausgabe auf ihren eigentlichen Textinhalt reduziert.
- Jedes Zahlungsdokument erhält ein neues Fälligkeitsdatum bezogen auf sein eigenes Erstellungsdatum.
- Standard: 14 Tage nach Dokumenterstellung.
- Der Wert bleibt über `Einstellungen → Workflows → Zahlungsworkflow → Zahlungsziel` anpassbar.
- Das Datum wird deutsch als `TT.MM.JJJJ` und englisch als `MM/TT/JJJJ` ausgegeben.

## Test

1. Zahlungserinnerung neu öffnen/erzeugen.
2. Prüfen, dass Name und Anschrift korrekt erscheinen und kein `b'1'` sichtbar ist.
3. Prüfen, dass das Fälligkeitsdatum 14 Tage nach dem aktuellen Erstellungsdatum liegt.
4. Zahlungsziel in den Workflow-Einstellungen ändern und ein weiteres Schreiben erzeugen.
5. Mahnung 1–3 sowie Inkassohinweis ebenfalls öffnen und prüfen.
6. Abschließend PAC-Prüfung durchführen.
