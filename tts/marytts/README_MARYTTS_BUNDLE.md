# MaryTTS Bundle Folder

GAM 2.0 Schritt 33c erwartet hier ein lokales MaryTTS-Bundle.

Wenn ein vollständiges MaryTTS-Paket hier abgelegt wird, versucht GAM beim Backend-Start automatisch MaryTTS zu starten.

Erwartete Launcher:

Windows:
- `bin/marytts-server.bat`
- oder `marytts-server.bat`
- oder `start-marytts.bat`

Linux/macOS:
- `bin/marytts-server`
- oder `start-marytts.sh`

Standard-Port:
- `59125`

Standard-Endpunkt:
- `http://localhost:59125/process`

Hinweis:
Aus Lizenz- und Paketgrößengründen enthält dieses Demo-ZIP noch keine vollständigen MaryTTS-Voice-Dateien.
Die Anwendung ist aber so vorbereitet, dass MaryTTS ohne weitere GAM-Konfiguration automatisch genutzt wird, sobald das Bundle in diesem Ordner vorhanden ist.
