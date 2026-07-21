# Schritt 40k30 – Inventar-Stabilisierung und Bedienung

- Alle von Discovery bereits als registriert erkannten Geräte bleiben in der unteren Geräteliste sichtbar.
- Die Deduplizierung verwendet nur noch belastbare Identitäten (IP sowie normalisierte Seriennummer/MAC), nicht mehr bloß gleiche oder generische Namen.
- Registrierte Discovery-Geräte ohne aktuell geladenen Stammdatensatz werden als Fallback einsortiert und gezählt.
- Neue Ansichtsfilter unterscheiden alle registrierten Einträge, gespeicherte Stammsätze und Discovery-Fallbacks.
- Deregistrierung bleibt für moderne Geräte und Discovery-Fallbacks direkt in der Geräteliste verfügbar.
- Der große Detailpfeil ist vollständig von Zeilenklick und Doppelklick entkoppelt (`preventDefault`, `stopPropagation`, MouseDown- und Doppelklick-Sperre).
- Einfacher Zeilenklick lädt Details ohne Bearbeitungsdialog; erst Doppelklick oder „Bearbeiten“ öffnet den Dialog.
- Discovery-Aktualisierungen mit registrierten Treffern lösen zusätzlich eine Inventar-Neuladung aus.
- Windows-Konsolenmeldungen der mDNS-Phase bleiben ASCII-sicher.
