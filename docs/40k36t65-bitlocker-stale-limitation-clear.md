# 40k36t65 – veraltete BitLocker-Einschränkung löschen

- Ursache: Discovery-Protokolldetails werden labelweise zusammengeführt.
- Wenn ein neuer erfolgreicher Lauf `Berechtigungseinschränkungen` nicht mehr sendet,
  blieb der alte Wert (z. B. `Ungültiger Namespace`) erhalten.
- Windows sendet bei einem sauberen Lauf jetzt explizit
  `Berechtigungseinschränkungen: keine`.
- Dadurch überschreibt der aktuelle Lauf den historischen Fehler.
- Bei erfolgreichem BitLocker-Lesepfad wird ein zuvor gesetzter BitLocker-Fehler
  innerhalb derselben Prüfung verworfen.
- Das Frontend behandelt `keine` als sauberen Zustand und zeigt weder Warnhinweis
  noch Einschränkungszeile an.
- Dadurch verschwindet derselbe veraltete generische Hinweis auch aus dem macOS-
  Berechtigungsbereich des zusammengeführten Gerätedatensatzes.
