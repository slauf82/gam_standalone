-- Schritt 34y: schneller Kontrollblock für die Translation-Tabellen
-- Ziel: gleiche Key-Basis prüfen, ohne Daten zu verändern.

SELECT 'translation_german' AS table_name, COUNT(*) AS rows_total, COUNT(DISTINCT TRANSLATE_DESCRIPTION) AS descriptions_distinct FROM translation_german
UNION ALL SELECT 'translation_english', COUNT(*), COUNT(DISTINCT TRANSLATE_DESCRIPTION) FROM translation_english
UNION ALL SELECT 'translation_french', COUNT(*), COUNT(DISTINCT TRANSLATE_DESCRIPTION) FROM translation_french
UNION ALL SELECT 'translation_ukrainian', COUNT(*), COUNT(DISTINCT TRANSLATE_DESCRIPTION) FROM translation_ukrainian
UNION ALL SELECT 'translation_italian', COUNT(*), COUNT(DISTINCT TRANSLATE_DESCRIPTION) FROM translation_italian
UNION ALL SELECT 'translation_swedish', COUNT(*), COUNT(DISTINCT TRANSLATE_DESCRIPTION) FROM translation_swedish
UNION ALL SELECT 'translation_turkish', COUNT(*), COUNT(DISTINCT TRANSLATE_DESCRIPTION) FROM translation_turkish
UNION ALL SELECT 'translation_russian', COUNT(*), COUNT(DISTINCT TRANSLATE_DESCRIPTION) FROM translation_russian;

-- Deutsche Fehlfüllungen in neuen Sprachen finden: gleicher Key, gleicher Text wie Deutsch.
SELECT 'italian' AS lang, i.ID, i.TRANSLATE_DESCRIPTION, i.TRANSLATED_TEXT
FROM translation_italian i
JOIN translation_german g ON SUBSTRING_INDEX(i.TRANSLATE_DESCRIPTION,'.',-1)=SUBSTRING_INDEX(g.TRANSLATE_DESCRIPTION,'.',-1)
WHERE TRIM(LOWER(i.TRANSLATED_TEXT)) = TRIM(LOWER(g.TRANSLATED_TEXT))
UNION ALL
SELECT 'swedish', s.ID, s.TRANSLATE_DESCRIPTION, s.TRANSLATED_TEXT
FROM translation_swedish s
JOIN translation_german g ON SUBSTRING_INDEX(s.TRANSLATE_DESCRIPTION,'.',-1)=SUBSTRING_INDEX(g.TRANSLATE_DESCRIPTION,'.',-1)
WHERE TRIM(LOWER(s.TRANSLATED_TEXT)) = TRIM(LOWER(g.TRANSLATED_TEXT))
UNION ALL
SELECT 'turkish', t.ID, t.TRANSLATE_DESCRIPTION, t.TRANSLATED_TEXT
FROM translation_turkish t
JOIN translation_german g ON SUBSTRING_INDEX(t.TRANSLATE_DESCRIPTION,'.',-1)=SUBSTRING_INDEX(g.TRANSLATE_DESCRIPTION,'.',-1)
WHERE TRIM(LOWER(t.TRANSLATED_TEXT)) = TRIM(LOWER(g.TRANSLATED_TEXT))
UNION ALL
SELECT 'russian', r.ID, r.TRANSLATE_DESCRIPTION, r.TRANSLATED_TEXT
FROM translation_russian r
JOIN translation_german g ON SUBSTRING_INDEX(r.TRANSLATE_DESCRIPTION,'.',-1)=SUBSTRING_INDEX(g.TRANSLATE_DESCRIPTION,'.',-1)
WHERE TRIM(LOWER(r.TRANSLATED_TEXT)) = TRIM(LOWER(g.TRANSLATED_TEXT));
