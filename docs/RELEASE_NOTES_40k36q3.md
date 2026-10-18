# 40k36q4b – Mehrfach-Betriebssystemzugänge

- WinRM, SSH und Android ADB unterstützen jetzt mehrere parallel gespeicherte Zielzugänge.
- Jeder Zugang besitzt Bezeichnung, Host/IP, Port, Aktivstatus und eigene Zugangsdaten.
- SSH unterstützt je Eintrag Passwort oder Schlüsselpfad.
- WinRM unterstützt je Eintrag HTTP/HTTPS und eigene Anmeldedaten.
- ADB speichert je Eintrag Host, Verbindungsport, optional Pairing-Port/-Code und Geräteidentität.
- Jeder gespeicherte Zugang kann direkt einzeln getestet und gelöscht werden.
- Linux/macOS-SSH und Windows-WinRM lösen bei Remote-Inventarisierungen automatisch den zum Zielhost passenden Zugang auf.
- Die bisherigen globalen Einzelkonfigurationen bleiben rückwärtskompatibel als Fallback erhalten.
