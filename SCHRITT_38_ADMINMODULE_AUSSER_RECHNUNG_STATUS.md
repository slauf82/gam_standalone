# Schritt 38 – Administrationsbereiche außerhalb des Rechnungsmoduls

## Basis

- GAM 2.0 v1.7.4 finaler Stand mit OpenHTMLtoPDF, PDF/A-3, ZUGFeRD, PDF/UA und WCAG
- GAM 1.0 Quellen aus `JsfJpaCrud_V13.zip`
- Datenbankschema aus `kopfzentruminventardb.sql`

## Ziel

Die einfachen Administrationsbereiche der historischen GAM-1.0-Module werden in GAM 2.0 wieder sichtbar und bearbeitbar gemacht. Die Rechnungsadministration bleibt bewusst ausgespart und wird später als eigener finaler Block umgesetzt.

## Neu

### Backend

Neuer REST-Bereich:

```text
/api/gam/admin/masterdata
```

Enthaltene Endpunkte:

```text
GET  /catalogs
GET  /{key}?q=&limit=
POST /{key}
PUT  /{key}/{id}
```

Der Zugriff ist auf Admin-/Superadmin-Rollen begrenzt.

### Neue Adminbereiche

- Filialen / Standorte
- Geräte neu
- Arbeitsplätze
- Personal
- Lagerartikel
- Verbrauchsmaterial
- Aufgaben
- Freigaben
- Kassenbuch
- Preisliste
- Softwareauswahl
- Geräteprüfungen
- Inbetriebnahmen
- Einweisungen

### Frontend

Neuer Menüpunkt:

```text
Modul-Admin
```

Funktionen:

- Adminbereich auswählen
- Datensätze suchen
- Datensätze anzeigen
- Datensatz laden
- neuen Datensatz anlegen
- Datensatz bearbeiten
- speichern

## Bewusst nicht enthalten

- Rechnungsadministration
- Rechnungsgesellschaften
- Rechnungstexte
- Rechnungsanreden
- Rechnungsgrüße
- rechtliche Hinweise
- Rechnungsnummernkreise
- PDF-/Portal-/ZUGFeRD-Konfiguration

Diese Bereiche bleiben für den späteren finalen Schritt der Rechnungsadministration reserviert.

## Ergebnis

Mit Schritt 38 werden die übrigen, einfacheren Adminmodule weitgehend aus GAM 1.0 nach GAM 2.0 übernommen. Danach kann die Roadmap gegenüber GAM 1.0 praktisch auf den letzten großen Block eingegrenzt werden:

```text
Administration Rechnungsmodul
```

## Build-Hinweis

Maven konnte in der ChatGPT-Umgebung nicht ausgeführt werden, weil kein lokaler Maven vorhanden war und der Downloadzugriff auf Maven Central blockiert ist.
