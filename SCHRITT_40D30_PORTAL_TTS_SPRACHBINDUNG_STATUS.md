# Schritt 40d30 – Patientenportal: TTS-Technik und PDF-Sprache

- Beide Vorleseaktionen verwenden dieselbe aktuell ausgewählte Vorlesetechnik.
- Piper TTS bleibt Standard beim ersten Portalaufruf.
- Die Auswahl Piper/Browser wird lokal gespeichert und bei beiden Aktionen ausgewertet.
- Ein Wechsel der Technik stoppt eine eventuell laufende Ausgabe sauber.
- Rechnung vorlesen verwendet die ausgewählte Rechnungs-/PDF-Sprache für PDF-Erzeugung, Textextraktion und Stimme.
- Portalinformationen vorlesen verwendet ebenfalls die Stimme der ausgewählten Rechnungs-/PDF-Sprache.
- Piper erhält den vollständigen ausgewählten Sprachcode; Browser-TTS erhält das passende Sprachgebietsschema.
- Piper-Ausfall fällt weiterhin auf Browser-TTS zurück.
