# 40k36t98 – PowerShell-5.1-kompatible Pipeline

t97 zeigte im SSPI-Datei-Launcher einen ParserError `EmptyPipeElement`.
Ursache: Die Pipelineoperatoren standen jeweils am Anfang der Folgezeile.
Diese Schreibweise wird erst von PowerShell 7 unterstützt.

t98 setzt den Pipelineoperator jeweils ans Ende der vorherigen Zeile:

    command |
      ForEach-Object ... |
      Out-File ...

Damit bleibt die mehrzeilige Pipeline mit Windows PowerShell 5.1 kompatibel.

Sonst unverändert:
- SSPI/PSSession-Dateitransport
- t95-Stufenmarker und harte Prüfungen
- t96 PS1-Dateilauncher
- t97 BOM-/Encoding-sicheres Einlesen
- Legacy-NTLM-Pfad für Linux/macOS
