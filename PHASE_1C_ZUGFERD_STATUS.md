# GAM Standalone – Phase 1C neu: ZUGFeRD/Factur-X als Pflicht-Export

Stand: 2026-05-23

## Entscheidung

Der PDF-Export ist im Rechnungsmodul nicht mehr nur ein normaler PDF-Download.
Ab dieser Phase ist der primäre Export:

```text
sichtbare Rechnung als PDF
+
eingebettete ZUGFeRD/Factur-X XML
=
E-Rechnung als PDF/A-3-Zielformat
```

Der alte normale PDF-Export bleibt nur als Debug-/Fallback-Endpunkt erhalten.

## Neue Backend-Struktur

Neue Services:

- `ZugferdXmlService`
  - erzeugt die maschinenlesbare CII-/EN16931-XML aus den Rechnungsdaten
  - nutzt Rechnungspositionen, Summen, Gesellschaft und `.lbd`-Empfänger

- `ZugferdExportService`
  - rendert sichtbares PDF
  - erzeugt XML
  - bettet XML per Mustangproject in das PDF ein
  - liefert fertige ZUGFeRD/Factur-X-PDF-Rechnung

- `ZugferdStatus`
  - Diagnose/Status des E-Rechnungs-Exports

## Neue/angepasste Endpunkte

```text
GET /api/invoices/{number}/pdf
```

liefert jetzt die ZUGFeRD/Factur-X-PDF-Rechnung.

```text
GET /api/invoices/{number}/pdf-debug
```

liefert nur das sichtbare Fallback-PDF ohne XML-Einbettung.

```text
GET /api/invoices/{number}/zugferd.xml
```

liefert die erzeugte XML separat zur Prüfung.

```text
GET /api/invoices/zugferd/status
```

liefert Status/Profil/Validierungseinstellung.

## Neue Konfiguration

```yaml
zugferd:
  enabled: true
  profile: EN16931
  validate: false
  seller-country: DE
  buyer-country: DE
  buyer-email: ""
```

Umgebungsvariablen:

```bash
GAM_ZUGFERD_ENABLED=true
GAM_ZUGFERD_PROFILE=EN16931
GAM_ZUGFERD_VALIDATE=false
GAM_ZUGFERD_SELLER_COUNTRY=DE
GAM_ZUGFERD_BUYER_COUNTRY=DE
GAM_ZUGFERD_BUYER_EMAIL=
```

## Bibliothek

Verwendet wird Mustangproject:

```xml
<dependency>
  <groupId>org.mustangproject</groupId>
  <artifactId>library</artifactId>
  <version>2.23.1</version>
</dependency>
```

## Wichtig: Validierung

Die harte Validierung ist in dieser Phase noch vorbereitet, aber nicht erzwungen.
Der Grund: Zuerst soll der Exportpfad stabil laufen, danach gleichen wir XML-Felder, Steuerlogik, Pflichtfelder und Layout gegen echte Altrechnungen ab.

Nächster Schritt nach dieser Phase:

1. echte Beispielrechnung aus altem GAM vergleichen
2. XML gegen Validator prüfen
3. Pflichtfelder ergänzen
4. Validierung hart aktivieren
5. altes Layout nachziehen

## Fachliche Leitlinie

ZUGFeRD/Factur-X ist ab jetzt kein optionales Add-on mehr, sondern Kernbestandteil des Rechnungsmoduls.
