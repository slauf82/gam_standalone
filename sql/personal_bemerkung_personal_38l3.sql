-- GAM 2.0 Schritt 38l3
-- HR-/IT-Trennung in der gemeinsamen Tabelle personal.
-- Neue HR-Bemerkung direkt hinter TELEFON2.
-- Die vorhandene Spalte BEMERKUNG bleibt als IT-/Superadmin-Bemerkung bestehen.

ALTER TABLE personal
  ADD COLUMN IF NOT EXISTS BEMERKUNG_PERSONAL TEXT NULL AFTER TELEFON2;
