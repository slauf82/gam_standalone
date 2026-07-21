# GAM – Architekturgrundsätze

## Lizenzstrategie
GAM bleibt derzeit unter GNU GPL v3. Die Architektur soll eine spätere kommerzielle Zusatzlizenz beziehungsweise Dual-Licensing ermöglichen. Beiträge Dritter müssen deshalb mit klarer Rechteübertragung oder einem Contributor Agreement dokumentiert werden, bevor eine proprietäre Parallel-Lizenzierung darauf gestützt wird.

## Modulprinzip
Module sind einzeln aktivierbar. GAM 3.0 soll Module getrennt starten, aktualisieren und neu laden können.

## Workflowprinzip
Fachprozesse verwenden gemeinsame Status-, Einstellungs-, Aufgaben-, Historien- und Benachrichtigungsbausteine. Der Workflow gehört sichtbar zum jeweiligen Fachobjekt.

## Dokumentenprinzip
Workflow-Dokumente verwenden dieselbe zentrale Dokumentenengine wie Rechnungen: konsistentes Layout, Mehrsprachigkeit, PDF/UA-orientiertes Tagging, WCAG/PAC-Prüfbarkeit, Portalbereitstellung und Vorlesefunktion.

## UI-Prinzip
Einstellungen verwenden gruppierte Karten, bündige Beschriftungen, einheitliche Checkboxraster und kompakte Zahlenfelder mit außenstehenden Einheiten.

## Statusfarben
Rot, Gelb und Grün sind ausschließlich fachlichen Zuständen vorbehalten. Dekorative Modulfarben verwenden andere Farbräume.

## API-Prinzip
Neue APIs sollen langfristig versionierbar bleiben. Fachcontroller kapseln Berechtigungen und Datenzugriffe serverseitig.

## Offline-first
Kernfunktionen müssen lokal und ohne Cloud funktionieren. Cloud- und Portaloptionen sind Ergänzungen, keine Voraussetzung.
