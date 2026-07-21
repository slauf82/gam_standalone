# Schritt 40k30k – sofortige Live-Suche und sichtbare Accordion-Pfeile

- Der direkte POST-Streaming-Pfad wurde deaktiviert, weil er in der Servlet-/Security-Kette bis zum globalen Abschluss gepuffert werden konnte.
- Das Frontend legt nun nur eine Discovery-Session an und öffnet anschließend den SSE-Kanal.
- Erst der erfolgreich abonnierte SSE-Kanal startet genau einen Discovery-Worker.
- Fortschritt, Diagnosen und Treffer werden über `SseEmitter` unmittelbar an das Frontend gesendet.
- Der alte direkte Streaming-Endpunkt kann keinen zweiten oder gepufferten Suchlauf mehr auslösen.
- Registrierungsmodus, Erkennungsquellen und sämtliche Unterbereiche besitzen wieder explizite, sichtbare Pfeile, die sich beim Öffnen drehen.
