# Schritt 28e – WYSIWYG-Rechnungsvorschau

Ziel: Die rechte Vorschau im Rechnungseditor soll verbindlich zeigen, was später in der PDF-Rechnung erscheint.

## Enthalten

- Erweiterte rechte Rechnungsvorschau
- Anzeige von Gesellschaft, Belegnummer, Rechnungsdatum, PDF-Sprache und Zahlungsart
- Empfängerblock aus LBD-Daten
- Anrede und Rechnungstext über vorhandene translation_* Tabellen
- Positionstabelle mit Menge, Code, Beschreibung, MwSt, Einzelpreis und Gesamtpreis
- Rabatt-/Gutschein-/Endbetrag-Zeilen nur wenn tatsächlich aktiv
- Ratenzahlungsinformation in der Vorschau
- Rechtlicher Hinweis und Grußformel aus Translation-Schlüsseln
- Bank-/IBAN-/BIC-/Steuerdaten in der Vorschau
- Erweiterte Übersetzungs-Labels für DE/EN/FR/UK

## Zielbild

Vorschau == PDF-Inhalt vor dem Erzeugen.

## Noch offen

- Vollständige 1:1-Pixel-/Layout-Gleichheit zur PDF
- PDF/UA und Vorlesbarkeit folgt in Schritt 28f
- Komplette UI-Übersetzung aller Module folgt später
