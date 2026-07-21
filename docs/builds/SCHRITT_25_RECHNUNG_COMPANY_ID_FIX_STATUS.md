# Schritt 25 – Kritischer Fix: Rechnung eindeutig über Gesellschaft + Nummer

## Anlass

In mehreren Rechnungsgesellschaften können identische Rechnungsnummern vorkommen. Eine Aktion nur über die Rechnungsnummer kann deshalb die falsche Rechnung treffen.

Beispiel:

- Gesellschaft A: Rechnung 3643
- Gesellschaft B: Rechnung 3643

Eine Stornierung von Gesellschaft A darf niemals eine Rechnung mit gleicher Nummer aus Gesellschaft B verwenden.

## Enthaltene Anpassungen

- Rechnungsladen unterstützt jetzt `companyId` zusätzlich zur Rechnungsnummer.
- Storno wird mit `number + companyId` ausgeführt.
- Gutschrift wird mit `number + companyId` ausgeführt.
- Rechnungspositionen werden beim Laden und Kopieren nach Gesellschaft gefiltert.
- Storno-/Gutschrift-Erzeugung übernimmt die Gesellschaft der eindeutig gefundenen Originalrechnung.
- Das Frontend übergibt bei Storno/Gutschrift die `companyId` der aktuell ausgewählten Rechnung.
- Die Suche bleibt gesellschaftsgeführt.

## Fachregel

Eine Rechnung ist fachlich eindeutig durch:

```text
Gesellschaft + Rechnungsnummer
```

Technisch soll langfristig die interne Rechnungs-ID bevorzugt werden. Schritt 25 verhindert zunächst die kritische Fehlzuordnung bei Storno und Gutschrift.

## Testfall

1. Gesellschaft Aqua Medical Ästhetics auswählen.
2. Rechnung 3643 suchen und öffnen.
3. Storno auslösen.
4. Erwartung: Es wird 3643S für dieselbe Gesellschaft erstellt.
5. Es darf keine 3643S der Kopfzentrum Betriebsgesellschaft Leipzig mbH angezeigt oder verwendet werden.
