# Schritt 40k30c – Registrierungsmodus und automatische Kategoriezuordnung

## Umgesetzt

- Der Registrierungsmodus ist nicht mehr innerhalb der Erkennungsquellen einsortiert.
- Eigener, standardmäßig geöffneter Einstellungsbereich „Registrierungsmodus“.
- Klare Trennung:
  - Registrierungsmodus: Was geschieht nach der Erkennung?
  - Erkennungsquellen: Wo sucht GAM nach Geräten?
- Drei dauerhaft in der GAM-Datenbank gespeicherte Optionen:
  - Erkannte Geräte automatisch registrieren – Standard EIN
  - Registrierte Geräte automatisch in die Geräteliste übernehmen – Standard AUS
  - Geräte automatisch einer Gerätekategorie zuordnen – Standard EIN
- Abhängigkeit bleibt erhalten:
  - Automatische Übernahme in die Geräteliste aktiviert automatisch die Registrierung.
  - Deaktivieren der automatischen Registrierung deaktiviert auch die automatische Inventarübernahme.
- Bei deaktivierter automatischer Kategoriezuordnung verwendet GAM bei der Übernahme in die Geräteliste den von der Quelle gelieferten Gerätetyp.
- Bei aktivierter Kategoriezuordnung verwendet GAM die vorhandene Klassifizierungslogik aus Typ, Hersteller, Protokollen und Identitätsmerkmalen.
- Erfolgs- und Fehlermeldungen unterscheiden nun zwischen Registrierungsmodus und Discovery-Quellen.

## Standardworkflow

Erkannt → automatisch registriert → manuelle Übernahme in die Geräteliste

Damit bleiben Geräteidentitäten dauerhaft bekannt, während die eigentliche Geräteliste bewusst gepflegt wird.

## Prüfung

- Frontend-Vite-Build erfolgreich.
- TypeScript-Gesamtprüfung meldet bereits im Ausgangsprojekt vorhandene Fehler außerhalb von 40k30c.
- Backend-Maven-Build konnte in der Arbeitsumgebung nicht ausgeführt werden, weil Maven nicht installiert war und der Maven-Download wegen fehlender DNS-Auflösung nicht möglich war.
