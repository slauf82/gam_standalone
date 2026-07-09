# GAM 2.0

**Gesellschafts- und Administrations-Managementsystem**

GAM 2.0 ist die moderne Weiterentwicklung des ursprünglichen GAM-Systems auf Basis von Spring Boot und React.

Der Fokus liegt auf:
- zentraler Verwaltung
- digitalen Workflows
- Automatisierung
- Mehrsprachigkeit
- Sicherheit
- langfristiger Wartbarkeit

---

# Aktueller Stand: GAM 2.0.3

## Moderne Plattform

- Spring Boot Backend
- React Frontend
- MariaDB Unterstützung
- automatische Datenbankmigrationen
- modulare Architektur
- REST API
- moderne Weboberfläche

---

## Sicherheit

- Benutzerverwaltung
- Rollen- und Rechteverwaltung
- Modulrechte
- Zwei-Faktor-Authentifizierung (TOTP)
- Passkey / WebAuthn Unterstützung
- sichere lokale Anmeldung
- automatische Sicherheitsmigrationen

---

## Rechnungsverwaltung

Die Rechnungsverwaltung wurde vollständig aus GAM 1.0 übernommen und modernisiert.

Funktionen:

- Rechnungserstellung
- Rechnungsvorschau
- PDF-Erzeugung
- ZUGFeRD Unterstützung
- Proforma-Rechnungen
- Gutschriften
- Stornierungen
- automatische Nummernkreise
- gesellschaftsabhängige Rechnungsdaten

---

## Rechnungsadministration

Vollständig administrierbar:

- Gesellschaften
- Filialen
- Produkte
- Preise mit Gültigkeitszeitraum
- Mehrwertsteuer mit Historie
- Anreden
- Rechnungstexte
- rechtliche Hinweise
- Grußformeln
- Logos

Besonderheiten:

- zentrale Stammdaten
- Wiederverwendung von Texten und Logos
- automatische Fallbacks
- produktionssichere Standardwerte

---

## Mehrsprachigkeit

- mehrsprachige Oberfläche
- Übersetzungssystem
- Produktübersetzungen
- Rechnungstext-Übersetzung
- Sprachumschaltung
- Vorbereitung internationale Nutzung

---

## Barrierefreiheit & Ausgabe

- moderne PDF-Erstellung
- PDF/A Unterstützung
- PDF/UA Vorbereitung
- Vorlesefunktion
- Piper TTS Integration

---

## Administration

Vorhandene Module:

- Benutzer
- Rechte
- Gesellschaften
- Filialen
- Geräte
- Inventar
- Lager
- Personal
- Patienten
- Termine
- Prüfungen
- Preislisten
- Kassenbuch

---

## Kommunikation

- internes Nachrichtensystem
- Login-News
- Aufgabenverwaltung
- Änderungsverlauf
- E-Mail Integration
- Bestellkommunikation

---

# Roadmap GAM 2.x – Workflow Generation

Nach Abschluss der Migration beginnt die Workflow-Erweiterung.

Ziel:

Nicht nur Daten verwalten, sondern Prozesse automatisieren.

---

## 40a – Rechnungsworkflow

Geplant:

Entwurf → Prüfung → Freigabe → Rechnung → Archiv

Funktionen:

- Rechnungsstatus
- Bearbeitungsverlauf
- automatische Aufgaben
- Freigaben

---

## 40b – Zahlungsworkflow

Geplant:

- offene Posten
- Zahlungseingänge
- Teilzahlungen
- Raten
- Zahlungsstatus

---

## 40c – Korrekturworkflow

Geplant:

- geführte Stornos
- Gutschriften
- Proforma-Prozesse
- Referenzierung von Originalbelegen

---

## 40d – Mahnwesen

Geplant:

- Zahlungserinnerungen
- Mahnstufen
- automatische Aufgaben
- editierbare Texte

---

## 40e – Freigabeworkflow

Geplant:

- Vier-Augen-Prinzip
- Genehmigungen
- Rollenfreigaben

---

## 40f – Änderungsworkflow

Geplant:

- Preisänderungen
- Gültigkeiten
- Historie
- Nachvollziehbarkeit

---

## 40g – Exportworkflow

Geplant:

- DATEV Export
- Excel Export
- Exportläufe
- Übergabehistorie

---

## 40h – Aufgaben & Kommunikation Workflow

Geplant:

- automatische Aufgaben
- interne Tickets
- Kommentare
- Verantwortlichkeiten
- Eskalationen

---

## 40i – Lager- und Materialflussworkflow

Geplant:

Digitale Warenbewegungen:

- Wareneingang
- Ausgabe
- Filialversorgung
- QR-/Barcode Scan
- Mindestbestände
- automatische Nachbestellungen

Ziel:

Artikel scannen → Menge → Standort → fertig

---

## 40j – Geräte Lifecycle Workflow

Geplant:

- Beschaffung
- Inventarisierung
- Ausgabe
- Rückgabe
- Aussonderung

---

## 40k – Mitarbeiter Workflow

Geplant:

Onboarding:

- Benutzer
- Rechte
- Geräte
- Arbeitsplatz

Offboarding:

- Rückgabe
- Rechte entfernen
- Abschlusskontrolle

---

## 40l – Prüfungs- und Wartungsworkflow

Geplant:

- Prüftermine
- Wartungen
- Erinnerungen
- automatische Aufgaben

---

## 40m – Beschaffungsworkflow

Geplant:

Bedarf → Freigabe → Bestellung → Lieferung → Lager

---

## 40n – Dokumentenworkflow

Geplant:

- Dokumentprüfung
- Freigaben
- Archivierung
- Ablaufüberwachung

---

## 40o – Workflow Engine

Langfristiges Ziel:

Regeln ohne Programmierung.

Beispiele:

Wenn:
Lagerbestand < Mindestbestand

Dann:
Aufgabe Einkauf erzeugen


Wenn:
Rechnung > Grenzwert

Dann:
Freigabe erforderlich

---

# Projektziel

GAM entwickelt sich von einer Verwaltungssoftware zu einer modularen Prozessplattform.

GAM 1:
Daten verwalten

GAM 2:
Daten verwalten, Dokumente erzeugen und Abläufe automatisieren.
Schritt 36i ergänzt die Rechnungserzeugung um PDF/UA-orientierte Barrierefreiheitsinformationen, ohne den ZUGFeRD/Factur-X-Export zu entfernen.

Enthalten:

- Dokumentensprache und PDF-Metadaten
- PDF/UA-XMP-Kennung
- MarkInfo und Tagged-PDF-Aktivierung, soweit von OpenPDF unterstützt
- Rollen für Tabellen, Tabellenzellen und Bilder
- Alternativtexte für Logo und QR-Code
- ZUGFeRD/Factur-X bleibt weiterhin eingebettet

Für die finale Freigabe wird eine Prüfung mit PAC 2024 oder Adobe Acrobat Preflight empfohlen. Weitere Hinweise siehe:

`tools/pdfua/README_PDF_UA_ZUGFERD.md`
