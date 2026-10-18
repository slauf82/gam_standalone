# 40k36q11 – Multi-Interface-Geräte

- Unterschiedliche IP- und MAC-Adressen bei gleichzeitig erreichbaren Treffern bleiben grundsätzlich ein Schutzsignal.
- Liegt jedoch starke gemeinsame Geräteidentität vor (gleiche Seriennummer oder Hardware-/System-UUID; alternativ stark corroborierte semantische Identität ohne widersprüchliche starke Merkmale), wird der Fall als Multi-Interface erkannt.
- Multi-Interface wird von KRITISCH auf HOCH abgestuft.
- Der Merge wird als Kandidat vorgeschlagen, aber niemals automatisch ausgeführt.
- Eine ausdrückliche manuelle Bestätigung bleibt zwingend.
- Nach dem Merge bleiben alle bekannten IP- und MAC-Adressen über die bestehende Netzwerkidentitätsverwaltung am gemeinsamen Gerät sichtbar.
- Im Gerätemanager heißt der allgemeine Hinweis bei offenen Merge-Kandidaten nun `Zusammenführung prüfen` statt `Konflikt offen`.
- Echte widersprüchliche Seriennummern, Build-Fingerprints, SSH-Hostkeys oder nicht plausibilisierte Mehrfach-IP/MAC-Fälle bleiben von der Multi-Interface-Ausnahme ausgeschlossen.
