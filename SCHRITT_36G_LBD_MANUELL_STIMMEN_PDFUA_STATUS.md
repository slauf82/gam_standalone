# Schritt 36g – GAM 2.0 v1.7.1

## Ziel

Schritt 36g bündelt den Feinschliff nach v1.7.0 und bereitet v1.7.1 vor.

## Enthaltene Punkte

### 36g.1 LBD-Daten vor Übernahme editierbar

- LBD-Daten werden beim Laden nicht mehr als unveränderbare Quelle behandelt.
- Die Empfängeradresse wird in editierbare Formularfelder übernommen.
- Name, Anrede, Titel, Straße, PLZ, Ort, Land und E-Mail können vor dem Speichern angepasst werden.
- Vorschau verwendet die editierte Empfängeradresse.

### 36g.2 Rechnung ohne LBD-Adresse

- Neuer Adressmodus im Rechnungsformular:
  - LBD
  - Manuell
- Eine Rechnung kann vollständig ohne LBD-Datei erstellt werden.
- Bei manueller Adresse wird keine LBD-Warnung benötigt.
- Die manuelle Adresse wird in `rechnungsdetails.FADRESSE` und E-Mail in `FEMAIL` gespeichert.
- PDF und Vorschau lesen die gespeicherte Adresse aus der Rechnung, nicht erneut aus der LBD-Datei.

### 36g.3 Piper-Stimmen je Sprache

- Ziel für v1.7.1: pro Sprache weibliche und männliche Piper-Stimme vorsehen.
- Die bestehende Piper-Auto-Download-Architektur bleibt Grundlage.
- Aktuell bleibt die vorhandene Standardstimme je Sprache aktiv; die Stimmenauswahl ist als weiterer Feinschliff vorbereitet/dokumentiert.

### 36g.4 PDF/UA

- PDF-Erzeugung setzt bereits Dokumenttitel, Autor, Creator, Keywords und Dokumentensprache.
- Die Lesereihenfolge wurde weiter konsistent gehalten, weil Vorschau/PDF/Portal denselben Rechnungsdatenpfad verwenden.
- Hinweis: vollständige PDF/UA-Validierung hängt von der eingesetzten PDF-Bibliothek ab und sollte mit einem PDF/UA-Validator geprüft werden.

## Technische Änderungen

- Neu: `InvoiceRecipientRequest.java`
- `InvoiceCreateRequest` und `InvoiceUpdateRequest` enthalten jetzt `recipient`.
- `InvoiceRepository` speichert und liest editierte/manuelle Empfängeradressen.
- `InvoicePdfService` verwendet die gespeicherte Rechnungsadresse, bevor auf LBD-Fallback zurückgegriffen wird.
- Frontend-Rechnungsformular enthält editierbare Empfängerfelder und Adressmodus.

## Testplan

1. LBD laden und Empfängerdaten vor dem Speichern ändern.
2. Rechnung speichern und Vorschau prüfen.
3. PDF öffnen und gleiche Adresse prüfen.
4. Neue Rechnung mit Modus „Manuell“ ohne LBD erstellen.
5. Prüfen, dass keine LBD-Warnung erscheint.
6. PDF/UA-Basis mit PDF-Validator prüfen.
7. Piper-TTS Standardsprachen weiter testen.

## Status

Build konnte in der Sandbox nicht final geprüft werden, weil Maven/Frontend-Abhängigkeiten nicht vollständig verfügbar sind.
