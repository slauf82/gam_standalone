# 40k36t68 – SYSTEM-Ergebnisübernahme und BitLocker-Zählung

- Aktuelle Windows-Berechtigungsfelder werden bei jedem verifizierten Windows-Inventarlauf
  als autoritativer Satz übernommen; alte Admin-/SYSTEM-Werte werden vor dem Merge entfernt.
- `Nicht verfügbare Module` wird bei 9/9 explizit als `keine` gespeichert, damit alte Werte
  wie `SECURITY` nicht stehen bleiben.
- `Berechtigungsmodus` wird zusätzlich als `SYSTEM`, `Administrator` oder `Standard` gespeichert.
- BitLocker-Zählung verwendet bevorzugt den Prozentwert. `0,0 %` zählt als nicht verschlüsselt.
- Deutsche Statusvarianten `Vollständig entschlüsselt` / `Vollstaendig entschluesselt`
  zählen ebenfalls als nicht verschlüsselt.
- SYSTEM-Launcher, UAC, Scheduled Task und Cleanup bleiben unverändert.
