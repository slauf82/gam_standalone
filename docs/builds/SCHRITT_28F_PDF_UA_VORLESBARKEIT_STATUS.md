# Schritt 28f – PDF/UA-Vorbereitung und Vorlesbarkeit

## Enthalten

- PDF-Metadaten für Titel, Autor, Betreff, Creator und Keywords
- PDF-Sprachkennung über BCP-47 (`de-DE`, `en-US`, `fr-FR`, `uk-UA`)
- Viewer-Präferenz `DisplayDocTitle`
- Lesereihenfolge im PDF weiterhin aus der WYSIWYG-Vorschau abgeleitet
- Vorlese-Button in der Rechnungsvorschau über Web Speech API
- Stop-Button für laufende Vorlesung
- Vorlesesprache abhängig von der gewählten PDF-Sprache

## Noch offen

- Vollständige PDF/UA-Validierung mit externem Validator
- Getaggte PDF-Struktur im engeren PDF/UA-Sinn
- Alternativtexte für Logos/Bilder, sobald Logos eingebunden sind
- Optional serverseitige Audio-Erzeugung, falls Browser-TTS nicht genügt
