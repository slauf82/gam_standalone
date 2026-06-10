# Schritt 28d – Translation Cache für neue Rechnungstexte

## Ziel

Neue im GAM-2.0-Rechnungsmodul eingeführte Texte dürfen nicht dauerhaft hart auf Deutsch erscheinen. Sie werden über Translation-Schlüssel geführt und bei fehlendem Datenbankeintrag automatisch in die bestehende `translation_*`-Tabelle geschrieben.

## Enthalten

- Nutzung der bestehenden Tabellen:
  - `translation_german`
  - `translation_english`
  - `translation_french`
  - `translation_ukrainian`
- Automatisches Anlegen fehlender Translation-Einträge bei Verwendung
- Offline-Fallbacktexte für neue Rechnungstexte in Deutsch, Englisch, Französisch und Ukrainisch
- Neue Schlüssel u. a. für:
  - Rabatt
  - Gutschein
  - Endbetrag nach Abzug
  - Ratenzahlung
  - Rechnungsvorschau
  - PDF-Metadaten
  - Bankverbindung
  - ZUGFeRD-Hinweis
  - Fallback-PDF-Hinweis
- PDF verwendet für neue Texte Translation-Schlüssel statt festem Deutsch
- Rechnungsvorschau verwendet sprachabhängige Labels
- Rabatt-/Gutschein-/Endbetrag-Zeilen werden nur angezeigt, wenn wirklich Rabatt oder Gutschein aktiv ist

## Noch offen

- echte LibreTranslate-Anbindung aktivieren
- Admin-Oberfläche zum Bearbeiten der Translation-Einträge
- vollständige UI-Übersetzung aller Masken
- gesellschaftsspezifische Textvarianten exakt wie im Alt-GAM rekonstruieren
