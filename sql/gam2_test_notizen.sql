-- GAM 2.0 Testnotizen
-- Diese Datei verändert absichtlich nichts automatisch.
-- Sie dient als Ort für lokale Testabfragen.

-- Anzahl Accounts
-- SELECT COUNT(*) FROM accounts;

-- Rollenübersicht
-- SELECT role, COUNT(*) FROM accounts GROUP BY role;

-- Rechnungen prüfen
-- SELECT COUNT(*) FROM rechnung;
-- SELECT * FROM rechnung ORDER BY 1 DESC LIMIT 10;

-- Rechnungsdetails prüfen
-- SELECT COUNT(*) FROM rechnungsdetails;

-- Geräte prüfen
-- SELECT COUNT(*) FROM geräte;
-- SELECT COUNT(*) FROM geräte_neu;

-- Lager/Material prüfen
-- SELECT COUNT(*) FROM lager;
-- SELECT COUNT(*) FROM verbrauchsmaterial;

-- GAM 2.0 Bewegungsjournal, falls bereits angelegt
-- SELECT * FROM gam_materialbewegung ORDER BY id DESC LIMIT 20;
