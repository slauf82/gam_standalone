# 40k36q1 – SSH-Test in Stufen

- SSH-Verbindungstest trennt jetzt TCP-Erreichbarkeit, SSH-Handshake und Authentifizierung.
- TCP-Porttest wartet bis zu 3 Sekunden und benötigt kein sofortiges SSH-Banner mehr.
- SSH-Handshake erhält bis zu 10 Sekunden.
- Authentifizierung erhält bis zu 15 Sekunden.
- Testbefehl erhält bis zu 10 Sekunden.
- Präzise Fehlermeldungen ersetzen die bisherige pauschale Meldung „SSH-Port geschlossen“.
- Betriebssystemzugänge zeigen TCP-Port, SSH-Handshake und Authentifizierung direkt im Testergebnis.
