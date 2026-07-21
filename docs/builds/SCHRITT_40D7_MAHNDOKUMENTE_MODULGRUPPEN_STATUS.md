# Schritt 40d7 – Mahndokumente und kompakte Modulgruppierung

- Zahlungsdokumente werden im Zahlungsworkflow authentifiziert als Blob geöffnet; dadurch entfällt der 403 bei direkten PDF-Links.
- Zahlungserinnerung, Mahnung 1–3 und Inkassoschreiben werden bei jedem Abruf aus den aktuellen Rechnungs- und Zahlungsdaten neu gerendert und wieder gespeichert. Dadurch werden auch bereits vorhandene leere PDFs repariert.
- Der Patientenportal-Abruf verwendet dieselbe Neurendering-Logik.
- Nach dem Login wird zuerst eine Modulgruppe ausgewählt. Sichtbar sind nur die Module dieser Gruppe.
- Beim Modulwechsel wird automatisch die zum aktuellen Modul passende Gruppe ausgewählt.
- Workflow-Module behalten ihren violett-blauen Rahmen.
