# Schritt 38k2 – Patienten/Termine Konsolidierung

- Basis bleibt Schritt 38k.
- Login-Direktzugriff auf Patientenverwaltung und Terminverwaltung korrigiert.
- Terminverwaltung bleibt vollständig enthalten.
- Patientenverwaltung übernimmt die adressen-basierte Logik aus 38j2.
- Bestehende Datensätze aus `adressen` werden über den vorhandenen MasterData-Katalog `patient-addresses` geladen.
- Termine verwenden dieselbe Patienten-/Adressbasis.
- Falls Backenddaten nicht erreichbar sind, bleibt der lokale Fallback erhalten.
