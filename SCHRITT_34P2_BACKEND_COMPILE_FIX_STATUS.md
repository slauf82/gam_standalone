# Schritt 34p2 – Backend Compile Fix

Basis: Schritt 34p

Korrektur:
- `UiTranslationService.findColumn(...)` wirft jetzt `SQLException` statt allgemeiner `Exception`.
- Dadurch sind die Aufrufe innerhalb des JDBC-Callbacks wieder Java-konform.
- Ziel: Backend-Kompilierung nach Schritt 34p wieder ermöglichen.

Unverändert:
- Browser-TTS bleibt auf dem stabilen Stand aus Schritt 34k.
- UI-Übersetzung nutzt weiterhin den deutschen Keykatalog aus `translate_GERMAN`.
- Zieltabellen bleiben im Alt-GAM-Schema, z. B. `ITALIAN.key`.
