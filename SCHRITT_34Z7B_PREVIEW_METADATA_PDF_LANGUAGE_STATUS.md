# Schritt 34z7b – Rechnungskopf/Vorschau-Metadaten nach PDF-Sprache

## Ziel

Kleiner Folgefix zu 34z7a:

- UI-Sprache und PDF-/Rechnungssprache bleiben getrennt.
- Bedienoberfläche, Buttons und Vorlese-/Stop-Texte bleiben UI-Sprache.
- Der obere Rechnungs-/Vorschau-Metadatenblock nutzt jetzt konsequent die PDF-/Rechnungssprache.

## Gefixt

Bei UI-Sprache Englisch und PDF-Sprache Deutsch standen zuvor im Rechnungsinhalt oben englische Labels:

- Invoice date
- Language
- payment method
- Treatment date
- Customer file
- User
- Invoice recipient

Diese Labels werden jetzt über die Rechnungssprache (`lang` / PDF-Sprache) statt über die Oberflächensprache gezogen.

## Unverändert

- Patientenportal-TTS aus 34z7 bleibt erhalten.
- Vorlesen/Stop-Kombibuttons bleiben erhalten.
- Produktbeschreibungen bleiben live übersetzt und werden nicht in die DB geschrieben.
- Login-/current-language-only-Logik bleibt unverändert.
