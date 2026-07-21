# Schritt 9 – Inventar + Lager verbinden

Diese Version verbindet die zuvor getrennten Bereiche Inventar/Geräte und Lager/Verbrauchsmaterial zu einem ersten nutzbaren Workflow.

## Neu im Backend

### Neue API-Gruppe

`/api/inventory-warehouse`

### Endpunkte

- `GET /api/inventory-warehouse/links`
  - listet Geräte-Material-Zuordnungen aus `geräte_neu_vmaterial`
  - Filter: `deviceId`, `materialId`

- `POST /api/inventory-warehouse/links`
  - legt eine Zuordnung Gerät → Verbrauchsmaterial an
  - nutzt die Originaltabelle `geräte_neu_vmaterial`

- `DELETE /api/inventory-warehouse/links/{linkId}`
  - entfernt eine Zuordnung

- `POST /api/inventory-warehouse/book`
  - bucht Verbrauchsmaterial als Zugang oder Entnahme
  - aktualisiert `verbrauchsmaterial.Anzahl`
  - legt bei Gerätebezug automatisch die Zuordnung Gerät → Material an
  - schreibt ein Bewegungsjournal

- `GET /api/inventory-warehouse/movements`
  - zeigt das Bewegungsjournal
  - Filter: `deviceId`, `materialId`, `limit`

## Neue interne Tabelle

Damit Bestandsbewegungen nachvollziehbar werden, erzeugt GAM 2.0 beim Start bei Bedarf:

```sql
CREATE TABLE IF NOT EXISTS gam_materialbewegung (...)
```

Die alte Datenbankstruktur bleibt dabei erhalten. Die neue Tabelle ergänzt nur das Bewegungsjournal.

## Wichtig korrigiert

Die alte Verbindungstabelle heißt laut SQL-Dump:

```text
geräte_neu_vmaterial
- ID
- GERÄTEID
- VMID
```

In der bisherigen Alpha-Version war teilweise noch ein falscher Spaltenname `rvmaterial_id` angenommen. Das ist in Schritt 9 korrigiert.

## Neu im Frontend

Im Lagerbereich erscheint zusätzlich:

- Inventar ↔ Lager
- Materialentnahme
- Materialzugang
- Zuordnung Gerät → Verbrauchsmaterial
- Anzeige der letzten Materialbewegungen

## Fachliche Bedeutung

Damit ist erstmals ein zusammenhängender Arbeitsfluss möglich:

```text
Gerät auswählen / kennen
→ Material zuordnen
→ Material entnehmen oder Zugang buchen
→ Bestand aktualisieren
→ Bewegung nachvollziehen
```

## Noch offen

- komfortable Geräte-/Materialauswahl per Suchdialog statt ID-Eingabe
- Reservierungen
- Mindestbestand/Warnungen
- Inventurmodus
- automatische Materialbuchung aus Rechnungs-/Serviceprozessen
- Bewegungsjournal auch für klassische `lager`-Artikel, nicht nur `verbrauchsmaterial`

## Build-Hinweis

In dieser Umgebung wurde weiterhin nicht kompiliert, weil Maven hier nicht installiert ist. Die Änderungen sind als nächste Arbeitsversion vorbereitet.
