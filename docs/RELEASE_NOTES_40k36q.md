# 40k36q – SSH 2.0 und halblokale Browsererkennung

- SSH für Linux und macOS auf einen eingebetteten Java-Client (Apache MINA SSHD 2.19.0) umgestellt.
- Kein externes `ssh.exe` bzw. `sshpass` mehr für normale SSH-Tests und Inventarisierungen erforderlich.
- Passwort- und Schlüsselanmeldung werden über dieselbe serverinterne SSH-Schicht ausgeführt.
- macOS-SSH-Test und macOS-Inventarisierung verwenden ebenfalls den eingebetteten SSH-Client.
- Halblokale Browsererkennung ergänzt: angemeldete GAM-Browser melden verfügbare Plattform-/Browserdaten an das Backend.
- Die Client-IP wird nicht aus Browserangaben übernommen, sondern serverseitig aus der tatsächlichen HTTP-Verbindung ermittelt.
- Browserdaten umfassen – soweit vom Browser freigegeben – Plattform, Plattformversion, Architekturhinweis, Modellhinweis, Browser/Version, Sprache, Zeitzone, Bildschirm, Touch, logische Prozessoren und RAM-Hinweis.
- Browsererkennung ist ausdrücklich vorläufig: Seriennummer, MAC-Adresse und echte Hardwareidentität werden erst über WinRM/SSH/ADB bzw. spätere Apple-Pairing-Zugänge verifiziert.
- Browser-Clients der letzten 24 Stunden werden als Discovery-Treffer in den normalen Suchlauf einbezogen und können mit späteren technischen Treffern derselben Identität zusammengeführt werden.
- Die Browsererkennung ist eine echte zusätzliche Discovery-Quelle; die bestehenden automatischen Quellenzähler erhöhen sich deshalb aus den Definitionslisten heraus.
