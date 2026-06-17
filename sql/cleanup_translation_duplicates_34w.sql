-- Schritt 34w: optionale Bereinigung alter Runtime-Übersetzungsduplikate.
-- Ausführen nur bei Bedarf, wenn durch 34v sehr viele doppelte/alte Einträge entstanden sind.
-- Die Tabellen behalten ihre vorhandenen Auto-Increment-IDs; fachlich maßgeblich bleibt TRANSLATE_DESCRIPTION.

-- Deutsche Fehlfüllung bei module.checks/module.compliance zurücksetzen, damit GAM neu übersetzen kann.
UPDATE translation_italian SET TRANSLATED_TEXT = 'Controlli'
WHERE TRANSLATE_DESCRIPTION IN ('ITALIAN.module.checks','ITALIAN.module.compliance')
  AND TRANSLATED_TEXT IN ('Prüfungen','Pruefungen');
UPDATE translation_swedish SET TRANSLATED_TEXT = 'Kontroller'
WHERE TRANSLATE_DESCRIPTION IN ('SWEDISH.module.checks','SWEDISH.module.compliance')
  AND TRANSLATED_TEXT IN ('Prüfungen','Pruefungen');
UPDATE translation_turkish SET TRANSLATED_TEXT = 'Kontroller'
WHERE TRANSLATE_DESCRIPTION IN ('TURKISH.module.checks','TURKISH.module.compliance')
  AND TRANSLATED_TEXT IN ('Prüfungen','Pruefungen');
UPDATE translation_russian SET TRANSLATED_TEXT = 'Проверки'
WHERE TRANSLATE_DESCRIPTION IN ('RUSSIAN.module.checks','RUSSIAN.module.compliance')
  AND TRANSLATED_TEXT IN ('Prüfungen','Pruefungen');

-- Falls Zwischenstände noch ITALIAN.UI.key usw. angelegt haben, können diese später
-- manuell gelöscht werden, wenn die passenden ITALIAN.key-Einträge vorhanden sind.
