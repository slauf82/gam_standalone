# Schritt 40k31.1 – Discovery-Stabilisierung

Umgesetzt:

- Die Einzelaktion „Registrierung aufheben“ verwendet jetzt einen body-basierten POST-Endpunkt. Zusammengesetzte Identitätsschlüssel mit Sonderzeichen werden nicht mehr als problematischer URL-Pfad übertragen; der bisherige 403-Fehler wird dadurch vermieden.
- „Registrierung aufheben und ignorieren“ verwendet dieselbe korrigierte Deregistrierungslogik und übernimmt das Gerät anschließend weiterhin in die lokale Ignorierliste.
- Geräte aus USB-, Windows-PnP- und Plug-and-Play-Quellen werden als „USB / Plug and Play“ gruppiert und erhalten ein eigenes Stecker-Symbol. Zusätzliche Fundquellen bleiben erhalten.
- Laufende Suchläufe können über „Gerätesuche abbrechen“ beendet werden.
- Der Backend-Endpunkt markiert die Discovery-Session als abgebrochen, unterbricht den Worker, publiziert ein `cancelled`-Ereignis und lässt bereits gefundene Geräte im Frontend stehen.
- Nach dem Abbruch kann unmittelbar eine neue Suche gestartet werden.

Prüfung:

- Frontend-Produktionsbuild erfolgreich.
- Der Maven-Backendbuild konnte in der isolierten Umgebung nicht ausgeführt werden, weil Maven nicht lokal vorhanden war und der Download wegen fehlender DNS-/Internetverbindung scheiterte.
