# Schritt 38d – Geräteverzeichnis vollständig

## Ziel

Das Geräteverzeichnis wurde vom bisherigen Lesemodus/Teilbetrieb in einen vollständigen Administrationsbetrieb überführt.

## Enthalten

- Geräteübersicht für Alt-GAM-Geräte und modernes `geräte_neu`-Modell
- Suche nach Geräten, Typ, Seriennummer, Inventarnummer, Hersteller, Standort und Filiale
- Filter nach Quelle (`alle`, `legacy`, `new`) und aktiven Geräten
- Detailansicht mit Bearbeitungsformular
- Neuanlage von Geräten im modernen `geräte_neu`-Modell
- Bearbeitung moderner Geräte
- Bearbeitung historischer Gerätefelder im Alt-GAM-Modell
- Aktiv-/Außerbetrieb-Status für Altgeräte
- Filialzuordnung für moderne Geräte
- Gesellschaftszuordnung für moderne Geräte
- Verbrauchsmaterial-/Lagerbezug für moderne Geräte
- Entfernen von Materialverknüpfungen
- Löschen moderner Geräte inklusive sauberer Entfernung abhängiger Zuordnungen
- Statistikbereich für Gerätebestand, Zuordnungen, Medizinprodukte, Elektrogeräte und Außerbetrieb-Status

## Technische Hinweise

- Die vorhandenen Tabellen `geräte`, `geräte_neu`, `filiale_geräte_neu`, `geräte_neu_vmaterial`, `filiale`, `rechnungsgesellschaft` und `verbrauchsmaterial` werden direkt verwendet.
- Alt-GAM-Geräte bleiben erhalten und werden nicht gelöscht, sondern nur bearbeitet bzw. außer Betrieb gesetzt.
- Das Löschen ist bewusst nur für moderne `geräte_neu`-Einträge aktiv.
- Der falsche Spaltenbezug `rgeräte_neu_id` wurde auf das reale Alt-GAM-Schema `GERÄTEID` korrigiert.

## Abgrenzung

Dieser Schritt schließt das Geräteverzeichnis ab. Die weiteren Fachmodule folgen in den nächsten Schritten 38e ff.

## Status

Umgesetzt, Build hier nicht geprüft, da Maven in der Ausführungsumgebung nicht installiert ist.
