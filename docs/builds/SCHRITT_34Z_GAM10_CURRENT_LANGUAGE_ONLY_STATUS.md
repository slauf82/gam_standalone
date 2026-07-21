# Schritt 34z – GAM-1.0-Logik für UI-Übersetzungen

Ziel: Die Übersetzungspflege kümmert sich wieder nur um die aktuell gewählte Oberflächensprache.

Änderungen:

- Keine Nachlade-Schleifen nach 3,5s/12s mehr im Frontend.
- Kein Vorbereiten mehrerer neuer Sprachen im Login- oder Modulstartpfad.
- Backend-Nachpflege bleibt auf den tatsächlich angefragten `lang`-Wert begrenzt.
- Login/Modulwechsel bleiben schnell.
- Browser-TTS bleibt unverändert auf dem stabilen 34k-Stand.

Gedanke:

Wie in GAM 1.0 gilt wieder:

```text
gewählte UI-Sprache -> Cache prüfen -> fehlende Texte dieser Sprache übersetzen -> speichern
```

Nicht mehr:

```text
Login -> mehrere Zielsprachen gleichzeitig vorbereiten
```
