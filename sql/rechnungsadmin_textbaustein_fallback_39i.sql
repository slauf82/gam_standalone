-- GAM 2.0 Schritt 39i
-- System-Fallback-Datensätze ID 0 für Rechnungs-Textbausteine.
-- Wichtig für MySQL/MariaDB: NO_AUTO_VALUE_ON_ZERO sorgt dafür, dass ID 0 wirklich gespeichert wird.

SET SESSION sql_mode = CONCAT_WS(',', @@sql_mode, 'NO_AUTO_VALUE_ON_ZERO');

CREATE TABLE IF NOT EXISTS `rechnungsanrede` (
  `ID` int NOT NULL AUTO_INCREMENT,
  `TEXT` text,
  PRIMARY KEY (`ID`)
);

CREATE TABLE IF NOT EXISTS `rechnungstext` (
  `ID` int NOT NULL AUTO_INCREMENT,
  `TEXT` text,
  PRIMARY KEY (`ID`)
);

CREATE TABLE IF NOT EXISTS `rechnungsrechtlicherhinweis` (
  `ID` int NOT NULL AUTO_INCREMENT,
  `TEXT` text,
  PRIMARY KEY (`ID`)
);

CREATE TABLE IF NOT EXISTS `rechnungsgrussformel` (
  `ID` int NOT NULL AUTO_INCREMENT,
  `TEXT` text,
  PRIMARY KEY (`ID`)
);

INSERT IGNORE INTO `rechnungsanrede` (`ID`, `TEXT`)
VALUES (0, 'Sehr geehrte Damen und Herren,');

INSERT IGNORE INTO `rechnungstext` (`ID`, `TEXT`)
VALUES (0, 'Wir erlauben uns folgende Leistungen in Rechnung zu stellen.');

INSERT IGNORE INTO `rechnungsrechtlicherhinweis` (`ID`, `TEXT`)
VALUES (0, 'Bitte begleichen Sie den Rechnungsbetrag innerhalb der angegebenen Frist.');

INSERT IGNORE INTO `rechnungsgrussformel` (`ID`, `TEXT`)
VALUES (0, 'Mit freundlichen Grüßen');
