# 40k36t93 – SSPI-Preflight + Legacy-Chunk-Vollinventar

Historische Erfolgslogs zeigen den robusten Vollinventar-Rückkanal über
Legacy winrm4j/CXF + NTLM + Chunk-Transfer. Der Windows-native SSPI-Fallback bleibt
für lokale Konten mit Leerzeichen als Authentifizierungsnachweis erhalten.

Routing:
1. SSPI-Preflight bestätigt Zugangsdaten.
2. Vollinventar verwendet bewusst `attemptLargePowerShellScript(...)` mit NTLM und
   90 Sekunden Timeout.
3. Nur falls der Chunk-Transport scheitert, wird SSPI noch einmal als letzte
   Vollinventar-Rückfallebene versucht.
4. Moderne Pfade gelten nur dann als erfolgreiches Vollinventar, wenn tatsächlich
   nicht-leere Nutzlast zurückkommt.

Unverändert bleiben t88 Identity-/Profilauflösung, der aktuelle modulare Windows-
Inventarisierungsumfang und die GAMKV-Struktur.
