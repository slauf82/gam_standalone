# Schritt 40k30g – echtes Live-Streaming, persistente Registrierung und klare Anzeige

- Discovery-Frontend verwendet wieder den direkten SSE-Stream des laufenden Suchrequests.
- Ein 16-KiB-SSE-Startkommentar durchbricht typische Servlet-/Proxy-Puffergrenzen.
- Treffer, Fortschritt und Diagnosen werden nach jedem Ereignis sofort geflusht und gerendert.
- Automatisch registrierte Geräte aktualisieren waehrend der Suche gedrosselt die dauerhaft aus der Datenbank geladene Liste „Registriert“.
- Discovery-Treffer und persistente Registrierung sind sprachlich getrennt:
  - „Neu erkannte Geräte“
  - „Bereits bekannte Treffer dieser Suche“
  - dauerhafte Liste „Registriert“
  - „Gerätebestand“
- Nach F5 ist die aktuelle Discovery leer; dauerhaft registrierte Geräte werden weiterhin aus `gam_discovery_registered_devices` geladen.
- „Erkennungsquellen“ und die untergeordneten Accordions besitzen wieder sichtbare Pfeile.
