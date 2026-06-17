-- Schritt 34y: Duplikate pro TRANSLATE_DESCRIPTION bereinigen, niedrigste ID bleibt erhalten.
-- Bitte vor Ausführung Datenbank sichern.

DELETE t1 FROM translation_italian t1 JOIN translation_italian t2
  ON t1.TRANSLATE_DESCRIPTION = t2.TRANSLATE_DESCRIPTION AND t1.ID > t2.ID;
DELETE t1 FROM translation_swedish t1 JOIN translation_swedish t2
  ON t1.TRANSLATE_DESCRIPTION = t2.TRANSLATE_DESCRIPTION AND t1.ID > t2.ID;
DELETE t1 FROM translation_turkish t1 JOIN translation_turkish t2
  ON t1.TRANSLATE_DESCRIPTION = t2.TRANSLATE_DESCRIPTION AND t1.ID > t2.ID;
DELETE t1 FROM translation_russian t1 JOIN translation_russian t2
  ON t1.TRANSLATE_DESCRIPTION = t2.TRANSLATE_DESCRIPTION AND t1.ID > t2.ID;
