# Schritt 38c – Phase A: Module inklusive Adminbereiche

Basis: Schritt 38b / GAM 2.0 v1.7.5.

Ziel dieses Schritts ist die weitere Annäherung an den GAM-1.0-Funktionsumfang außerhalb der Rechnungsadministration. Signatur und MOH-Auswertung bleiben bewusst ausgeschlossen.

## Ergänzt

- Patientenadressen / Patientenakte-Stammdaten (`adressen`)
- Textersetzungen (`replacement`)
- Übersetzungskataloge Deutsch, Englisch, Französisch, Ukrainisch
- Gesellschaft-Filiale-Zuordnung (`rechnungsgesellschaft_filiale`)
- Zahlungsavis-Positionen (`zahlungsavis`)

## Bereits aus 38/38b enthalten

- Filialen / Standorte
- Benutzer, Rollen, Anwendungen, Benutzer-Anwendungen
- Menübaum und Themes
- Geräte alt und Geräte neu
- Geräte-Filiale-Zuordnung
- Geräte-Verbrauchsmaterial-Zuordnung
- Arbeitsplätze
- Software und Softwareauswahl
- Ordnerfreigaben
- Personal und Mitarbeiterkürzel
- Aufgabenverwaltung
- Freigabemanagement
- Lager und Verbrauchsmaterial
- Kassenbuch und Namenskonten
- Preisliste
- Prüfungen, Inbetriebnahmen und Einweisungen
- News / Hinweise

## Bewusst nicht enthalten

- Signaturmodul: eingestelltes Testprojekt ohne vorhandenes Signaturpad
- MOH-Auswertung: separates eingestelltes Testprojekt
- Rechnungsadministration: bleibt finaler eigener Block

## Hinweis

Dieser Schritt erweitert die generische Moduladministration. Für einige historische Bereiche ist dies bewusst eine CRUD-/Stammdatenabdeckung und noch keine vollständige fachliche Spezialoberfläche wie in JSF/GAM 1.0.
