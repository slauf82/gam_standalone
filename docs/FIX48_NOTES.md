# GAM 2.1.0 – fix48

- Registrierte Gerätetabelle nutzt nun ein festes Tabellenlayout. Die zehn Spalten der Hauptzeile werden nicht mehr von der aufgeklappten Inventarisierungszeile bestimmt.
- Aufklapppfeil erhält eine konstante Spaltenbreite. Eingaben bleiben innerhalb ihrer Spalten.
- Detailzeile (colSpan=10) und Detailcontainer sind auf die verfügbare Tabellenbreite begrenzt. Lange Werte bleiben über Umbruch bzw. lokales Scrollen zugänglich.
- Ausschließlich Frontend-Dateien `frontend/src/main.tsx` und `frontend/src/style.css` geändert.
- Vorherige fix47-Korrekturen an WinRM, historischen Quellen, Plattformdarstellung und Merge nicht verändert.
- Sichtprüfung im Browser sowie TypeScript-Prüfung bitte im Zielsystem durchführen.
