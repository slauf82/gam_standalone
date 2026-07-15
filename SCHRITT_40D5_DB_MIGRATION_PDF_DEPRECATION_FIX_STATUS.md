# Schritt 40d5 – DB-Migration und PDF-Deprecation-Fix

## Behoben

- Die Marketing-Spalten `action_type_id`, `material_type_id`, `source_warehouse_id` und `target_branch_id` werden vor einer Migration über `information_schema.COLUMNS` geprüft.
- `ALTER TABLE ... ADD COLUMN` wird nur noch ausgeführt, wenn die jeweilige Spalte tatsächlich fehlt.
- Die MariaDB-Warnungen `1060-42S21 Duplicate column name` erscheinen bei weiteren Backend-Starts nicht mehr.
- Die veraltete, falsch geschriebene OpenHTMLtoPDF-Methode `usePdfUaAccessbility(...)` wurde durch `usePdfUaAccessibility(...)` ersetzt.
- Dieselbe Korrektur wurde sowohl im Rechnungs-PDF als auch bei Zahlungserinnerungen und Mahnungen vorgenommen.
- PDF/UA-Tagging, PDF/A-3-U und die bisherige Dokumentenlogik bleiben unverändert.

## Test

1. Backend zweimal nacheinander starten.
2. Beim zweiten Start dürfen für die vier Marketing-Spalten keine Duplicate-column-Warnungen erscheinen.
3. Backend mit Maven kompilieren.
4. Die Meldung über eine veraltete API in `InvoiceOpenHtmlPdfService.java` darf nicht mehr erscheinen.
5. Eine Rechnung sowie eine Zahlungserinnerung/Mahnung erzeugen und erneut mit PAC prüfen.
