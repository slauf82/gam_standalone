-- GAM 2.0 Schritt 39k
-- Rechnungsadministration: Logo-Normalisierung und Wiederverwendung
-- Logos werden als eigene Stammdaten geführt und per LOGO_ID mehreren Gesellschaften zugeordnet.

CREATE TABLE IF NOT EXISTS `rechnungslogo` (
  `ID` int NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) DEFAULT NULL,
  `URL` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`ID`)
);

ALTER TABLE `rechnungslogo` ADD COLUMN IF NOT EXISTS `NAME` varchar(255) DEFAULT NULL;
ALTER TABLE `rechnungsgesellschaft` ADD COLUMN IF NOT EXISTS `LOGO_ID` int(50) DEFAULT NULL;

SET SESSION sql_mode = CONCAT_WS(',', @@sql_mode, 'NO_AUTO_VALUE_ON_ZERO');
INSERT IGNORE INTO `rechnungslogo` (`ID`, `NAME`, `URL`)
VALUES (0, 'Systemstandard', 'KOPFZENTRUM_LOGO.png');
UPDATE `rechnungslogo`
SET `NAME` = COALESCE(NULLIF(`NAME`, ''), 'Systemstandard')
WHERE `ID` = 0;
