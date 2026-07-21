# Schritt 40k31d1 – Testanpassung semantischer Merge

Der bestehende Test für identische, markante Gerätenamen aus unabhängigen Quellen wurde an die mit 40k31d bewusst eingeführte Semantik angepasst.

- `OFFICE-PC` aus FRITZ!Box und `office-pc` aus Home Assistant wird jetzt als `AUTO_MERGE` erwartet.
- Der Testname beschreibt die neue fachliche Erwartung.
- Die Produktivlogik der Merge-Engine bleibt unverändert.

Damit entspricht die Testsuite der Entscheidung, markante normalisierte Gerätenamen aus unabhängigen Quellen automatisch zusammenzuführen.
