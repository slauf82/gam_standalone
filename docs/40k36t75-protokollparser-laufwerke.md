# 40k36t75 – Protokollparser für Laufwerkswerte korrigiert

- Discovery-Details werden nicht mehr blind an jedem ` · ` getrennt.
- Ein Separator gilt nur dann als echte Protokollgrenze, wenn danach ein plausibles neues `Label:` beginnt.
- Werte wie `E: NTFS · 346,2/578,9 GB frei` bleiben dadurch zusammen.
- Semikolon-getrennte Laufwerke erscheinen weiterhin zeilenweise.
- Laufwerksgrößen werden lesbarer formatiert:
  `346,2/578,9 GB frei` → `346,2 GB frei von 578,9 GB`.
- Inventarisierungswerte selbst werden nicht verändert.
- SYSTEM-, BitLocker-, Merge- und Plattformlogik aus t74 bleibt unverändert.
