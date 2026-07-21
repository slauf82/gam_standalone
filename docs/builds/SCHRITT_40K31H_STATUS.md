# Schritt 40k31h – Geräteanalyse und Qualitätsreports

## Neu

- gemeinsame Auswertung von Inventargeräten und registrierten Discovery-Geräten
- Kennzahlen für Gesamtbestand, Inventar, registrierte Geräte und manuelle Kategorien
- Vollständigkeitsgrad je Gerät
- Anzeige fehlender Kernfelder
- Kategoriebericht mit durchschnittlicher Datenqualität
- Quellenbericht mit Treffer- und Qualitätsquote
- heuristischer Dublettenbericht anhand Seriennummer, Adresse sowie Hersteller/Gerätename
- Registrierungs- und Änderungsübersicht
- Suchfilter über alle Geräteberichte
- CSV-Export des gefilterten Gerätequalitätsberichts
- alle Report-Unterbereiche standardmäßig eingeklappt

## Einordnung

40k31h schafft die Reportgrundlage für den Multi-Quellen-Gerätemanager. Die spätere Windows-Tiefenerkennung aus 40k32 kann die Qualitätswerte automatisch verbessern und zusätzliche Felder in die Vollständigkeitsprüfung einbringen.

## Buildhinweis

Im isolierten Arbeitscontainer waren keine `frontend/node_modules` vorhanden. Deshalb konnte der Vite-/TypeScript-Build dort nicht ausgeführt werden. Die Änderungen liegen vollständig im Quellprojekt vor.
