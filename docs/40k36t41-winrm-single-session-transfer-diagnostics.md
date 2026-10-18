# 40k36t41 – WinRM Legacy Single-Session Transfer

- Basis: 40k36t40.
- Der funktionierende Linux→Windows Legacy-NTLM-Authentifizierungspfad bleibt unverändert.
- Große PowerShell-Inventarskripte werden weiterhin Base64-gechunkt übertragen, jetzt aber vollständig über **eine einzige winrm4j/CXF-Session**.
- Transferstufen: `INIT`, `CHUNK n/N`, `RECONSTRUCT`, `EXECUTE`, `CLEANUP`.
- Bei Fehlern werden Stufe, Statuscode, stdout und stderr im GAM-Log ausgegeben.
- Chunkgröße 2000 Zeichen.
- Temporäre `.b64`- und `.ps1`-Dateien werden nach der Ausführung entfernt; Cleanup-Fehler beeinträchtigen das Inventarergebnis nicht.
- Wiederholte CXF `ReflectionServiceFactoryBean`-INFO-Ausgaben werden auf WARN reduziert, sofern `application.properties` verwendet wird.
- Linux/macOS-Skriptrechte werden beim ZIP-Build erneut explizit gesetzt.

- Zusätzlich wird die **Rückrichtung** segmentiert: Das Inventarskript schreibt seine Ausgabe zuerst in eine UTF-8-Ergebnisdatei auf Windows.
- Diese Ergebnisdatei wird Base64-kodiert in 1600-Zeichen-Blöcken über dieselbe WinRM-Session zurückgelesen und im GAM-Backend wieder zusammengesetzt.
- Damit hängt weder die Hin- noch die Rückrichtung von einer großen einzelnen WinRM-/SOAP-Ausgabe ab.
