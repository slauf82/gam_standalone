# GAM 2.0 – Schritt 39i – Rechnungsadministration Fallback & Produktionssicherheit

## Ziel

39i ergänzt die in 39h finalisierte Rechnungsadministration um eine robuste Fallback-Kette für die vier Rechnungs-Textbausteine.

## Umsetzung

- Für folgende Tabellen wird automatisch ein Systemdatensatz mit ID `0` angelegt:
  - `rechnungsanrede`
  - `rechnungstext`
  - `rechnungsrechtlicherhinweis`
  - `rechnungsgrussformel`
- MySQL/MariaDB wird für die Initialisierung mit `NO_AUTO_VALUE_ON_ZERO` betrieben, damit ID `0` wirklich als Primärschlüssel gespeichert wird.
- ID `0` wird in der Oberfläche als geschützter Systemstandard markiert.
- ID `0` kann weder geändert noch gelöscht werden.
- Vorschau und PDF nutzen dieselbe Fallback-Kette.

## Fallback-Kette

1. Gesellschaftsspezifische Textzuordnung verwenden.
2. Wenn keine Zuordnung vorhanden ist oder ein Textbaustein fehlt: Datensatz ID `0` der jeweiligen Tabelle verwenden.
3. Wenn sogar ID `0` fehlt oder die Datenbank beschädigt ist: interner Notfalltext im Java-Code.

## Notfalltexte

- Anrede: `Sehr geehrte Damen und Herren,`
- Rechnungstext: `Wir erlauben uns folgende Leistungen in Rechnung zu stellen.`
- Rechtlicher Hinweis: `Bitte begleichen Sie den Rechnungsbetrag innerhalb der angegebenen Frist.`
- Grußformel: `Mit freundlichen Grüßen`

## Build-Hinweis

Der Build konnte in dieser Umgebung nicht vollständig ausgeführt werden, weil Maven ohne vorhandenen lokalen Cache versucht hat, Maven/dependencies aus dem Internet nachzuladen. Die Änderungen wurden quellseitig integriert.
