# Schritt 40d32 – Mehrsprachige Vorschau, Portal-TTS und PDF-Logbereinigung

- Dokumentbereich der Vorschau verwendet ausschließlich die PDF-Sprache.
- Deutsche Administrationstexte werden nur für deutsche Dokumente verwendet; alle anderen Sprachen nutzen vollständige Sprachvorlagen.
- Sprachcodes de/en/fr/uk/it/sv/tr/ru/es/pt/nl/pl/cs werden zentral normalisiert.
- Patientenportal: Portalinformationen sprechen in Oberflächensprache, Rechnungen in PDF-Sprache.
- Der öffentliche Portalzugriff darf `/api/tts/audio` nutzen; Verwaltungs-/Installationsendpunkte bleiben geschützt.
- HTTP 403, fehlende Stimme, laufender Download und sonstige Piper-Fehler werden getrennt gemeldet.
- Piper unterstützt vorhandene Stimmen für alle konfigurierten Sprachen einschließlich Englisch.
- Nicht unterstütztes `word-break` wurde aus dem PDF-CSS entfernt.
- Nur der bekannte, technisch überholte OpenHTMLtoPDF-Beschreibungshinweis wird gefiltert; finale PDF/UA-Metadaten werden weiterhin gesetzt.
