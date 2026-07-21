# Schritt 40k18 – FRITZ!Box-Anreicherung und breite Geräteidentifizierung

## Umgesetzt

- optionale FRITZ!Box-Anbindung über TR-064
- Konfiguration direkt im Gerätemanager: Aktivierung, Host, Port, Benutzer, Passwort und Verbindungstest
- Passwort wird bei erneutem Laden nicht an das Frontend zurückgegeben
- Abruf von Gerätename, IP-Adresse, MAC-Adresse, Aktivstatus und Schnittstellentyp
- Zusammenführung primär über MAC-Adresse, ersatzweise über IP-Adresse
- reine lokale Discovery bleibt ohne FRITZ!Box vollständig funktionsfähig
- eigener Diagnoseabschnitt `FRITZBOX` im Discovery-Livestream
- breitere, herstellerunabhängige Kategorien für Praxen, Unternehmen, Vereine und weitere Einsatzfelder
- zusätzliche Klassen für Haushaltsgeräte, Robotik, Audio/Receiver sowie Klima- und Gebäudetechnik

## Zweck

40k18 ist bewusst nicht auf HNO-Praxen oder ein einzelnes Netzwerk zugeschnitten. Externe Datenquellen reichern die generische Discovery nur an. Die Geräteidentität bleibt modular, optional und für unterschiedliche Organisationen und Routerlandschaften verwendbar.
