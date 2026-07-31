-- ============================================================
-- GAM 2.1.0 Preview 2 – leere Datenbankstruktur
-- Quelle: bisherige anonymisierte Demo-Datenbank, erweitert für GAM 2.1.0 Preview 2
-- Hinweis: Tabellenstruktur, IDs und Fremdschlüssel bleiben erhalten.
-- Personen-, Gesellschafts-, Kontakt-, Bank- und Steuerdaten wurden ersetzt.
-- ============================================================

-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Erstellungszeit: 21. Jun 2026 um 00:50
-- Server-Version: 10.4.32-MariaDB
-- PHP-Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Datenbank: `gam_v2_1_0_preview2_empty`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `accounts`
--

CREATE TABLE `accounts` (
  `id` int(11) NOT NULL,
  `username` varchar(50) DEFAULT NULL,
  `password` varchar(100) DEFAULT NULL,
  `fullname` varchar(50) DEFAULT NULL,
  `role` varchar(50) DEFAULT NULL,
  `email` varchar(50) DEFAULT NULL,
  `secretkey` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `adressen`
--

CREATE TABLE `adressen` (
  `ID` int(100) NOT NULL,
  `PATIENTENNUMMER` varchar(100) DEFAULT NULL,
  `ANREDE` varchar(500) DEFAULT NULL,
  `TITEL` varchar(500) DEFAULT NULL,
  `VORNAME` varchar(500) DEFAULT NULL,
  `NAMENSZUSATZ` varchar(500) DEFAULT NULL,
  `NACHNAME` varchar(500) DEFAULT NULL,
  `STRASSE` varchar(100) DEFAULT NULL,
  `BUNDESLAND` varchar(100) DEFAULT NULL,
  `PLZ` varchar(100) DEFAULT NULL,
  `ORT` varchar(100) DEFAULT NULL,
  `LAND` varchar(100) DEFAULT NULL,
  `GEBDATUM` varchar(100) DEFAULT NULL,
  `VERSICHERTENNUMMER` varchar(100) DEFAULT NULL,
  `VERSICHERTENART` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `application`
--

CREATE TABLE `application` (
  `ID` int(11) NOT NULL,
  `APPLICATION` varchar(50) NOT NULL,
  `FG_SELECT` bit(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `arbeitsplatz`
--

CREATE TABLE `arbeitsplatz` (
  `ID` int(11) NOT NULL,
  `FILIALE_ID` int(11) NOT NULL DEFAULT 0,
  `ARBEITSPLATZ` varchar(50) DEFAULT NULL,
  `TELEFON` bit(1) DEFAULT NULL,
  `DATUM_EINRICHTUNG` date DEFAULT NULL,
  `DATUM_ANTRAGSTELLUNG` date DEFAULT NULL,
  `DATUM_ANTRAGGENEHMIGT` date DEFAULT NULL,
  `MITARBEITER` varchar(50) DEFAULT NULL,
  `FERTIGGESTELLT` bit(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `aufgaben`
--

CREATE TABLE `aufgaben` (
  `ID` int(11) NOT NULL,
  `USERNAME` varchar(50) DEFAULT NULL,
  `TAGESDATUM` date DEFAULT NULL,
  `KÜRZEL` varchar(50) DEFAULT NULL,
  `FILIALE_ID` int(11) DEFAULT NULL,
  `FACHBEREICH` varchar(50) DEFAULT NULL,
  `AUFGABE` varchar(500) DEFAULT NULL,
  `VERANTWORTLICHER` varchar(50) DEFAULT NULL,
  `PRIORITÄT` varchar(20) DEFAULT NULL,
  `STATUS` varchar(20) DEFAULT NULL,
  `FRIST` date DEFAULT NULL,
  `ERLEDIGT` varchar(50) DEFAULT NULL,
  `BEMERKUNG` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `einweisung`
--

CREATE TABLE `einweisung` (
  `EINWEISUNGS_ID` int(11) NOT NULL,
  `DATUMERSTEINWEISUNG` date DEFAULT NULL,
  `NAMEERSTEINGEWIESENER` varchar(50) DEFAULT NULL,
  `INBETRIEBNAHME_ID` int(11) NOT NULL,
  `DATUMFOLGEEINWEISUNG` date DEFAULT NULL,
  `NAMEFOLGEEINGEWIESENER` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `filiale`
--

CREATE TABLE `filiale` (
  `FILIALE_ID` int(11) NOT NULL,
  `FILIALEKUERZEL` varchar(50) NOT NULL,
  `FILIALENAME` varchar(200) NOT NULL,
  `ADR` varchar(200) DEFAULT NULL,
  `STRASSE` varchar(200) DEFAULT NULL,
  `PLZ` varchar(200) DEFAULT NULL,
  `ORT` varchar(200) DEFAULT NULL,
  `FADRESSE` bit(1) DEFAULT NULL,
  `EMAIL` varchar(50) DEFAULT NULL,
  `FEMAIL` bit(1) DEFAULT NULL,
  `RELEVANT` bit(1) DEFAULT NULL,
  `RGESELLSCHAFTS_ID` int(11) DEFAULT NULL,
  `NORMAL` bit(1) DEFAULT NULL,
  `KOSTENSTELLE` int(11) DEFAULT NULL,
  `KATEGORIE` varchar(50) DEFAULT NULL,
  `IMPORTORDNER` varchar(200) DEFAULT NULL,
  `EXPORTORDNER` varchar(200) DEFAULT NULL,
  `ACQ` bit(1) DEFAULT NULL,
  `AMAE` bit(1) DEFAULT NULL,
  `HCP` bit(1) DEFAULT NULL,
  `HCPa` bit(1) DEFAULT NULL,
  `EPBLN` bit(1) DEFAULT NULL,
  `KBL` bit(1) DEFAULT NULL,
  `KZB` bit(1) DEFAULT NULL,
  `EPGS` bit(1) DEFAULT NULL,
  `KZT` bit(1) DEFAULT NULL,
  `KZM` bit(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `filiale_geräte_neu`
--

CREATE TABLE `filiale_geräte_neu` (
  `ID` int(11) NOT NULL,
  `rfiliale_ID` int(11) DEFAULT NULL,
  `rgesellschafts_ID` int(11) DEFAULT NULL,
  `geräte_neu_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `freigabe`
--

CREATE TABLE `freigabe` (
  `ID` int(11) NOT NULL,
  `DATUM` date DEFAULT curdate(),
  `EINTRAGENDER` varchar(50) DEFAULT NULL,
  `BESCHREIBUNG` varchar(500) DEFAULT NULL,
  `GESELLSCHAFT_ID` int(11) DEFAULT NULL,
  `FILIALE_ID` int(11) DEFAULT NULL,
  `STATUS` varchar(50) DEFAULT '',
  `BEMERKUNG` varchar(500) DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `gam_materialbewegung`
--

CREATE TABLE `gam_materialbewegung` (
  `ID` bigint(20) NOT NULL,
  `CREATED_AT` timestamp NOT NULL DEFAULT current_timestamp(),
  `GERAET_ID` int(11) DEFAULT NULL,
  `MATERIAL_ID` int(11) NOT NULL,
  `DELTA` int(11) NOT NULL,
  `BESTAND_ALT` int(11) DEFAULT NULL,
  `BESTAND_NEU` int(11) DEFAULT NULL,
  `GRUND` varchar(255) DEFAULT NULL,
  `USERNAME` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `geräte`
--

CREATE TABLE `geräte` (
  `GERÄTE_ID` int(11) NOT NULL,
  `MEDGERÄTE` bit(1) DEFAULT NULL,
  `ELEKGERÄTE` bit(1) DEFAULT NULL,
  `INVENTAR` bit(1) DEFAULT NULL,
  `GeräteName` varchar(50) NOT NULL,
  `GeräteTyp` varchar(50) DEFAULT NULL,
  `Seriennummer` varchar(50) NOT NULL,
  `Anschaffungsdatum` date DEFAULT NULL,
  `Hersteller` varchar(100) DEFAULT NULL,
  `Inventarnummer` varchar(50) DEFAULT NULL,
  `INNERBETRIEBLICHER_STANDORT` varchar(50) DEFAULT NULL,
  `AUSSERBETRIEB` bit(1) DEFAULT NULL,
  `IMEINSATZ` bit(1) DEFAULT NULL,
  `Filiale` int(11) DEFAULT NULL,
  `BEMERKUNG` varchar(200) DEFAULT NULL,
  `URSPRUNGSFILIALE_ID` int(11) DEFAULT NULL,
  `BENANNTESTELLECE` varchar(50) DEFAULT NULL,
  `LIEFERANTENADRESSE` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `geräte_neu`
--

CREATE TABLE `geräte_neu` (
  `ID` int(11) NOT NULL,
  `Name` varchar(50) DEFAULT '',
  `Typ` varchar(50) DEFAULT 'Drucker',
  `Seriennummer` varchar(50) DEFAULT '',
  `IP` varchar(50) DEFAULT NULL,
  `Standort` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `geräte_neu_vmaterial`
--

CREATE TABLE `geräte_neu_vmaterial` (
  `ID` int(11) NOT NULL,
  `GERÄTEID` int(11) DEFAULT NULL,
  `VMID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `gesellschaft`
--

CREATE TABLE `gesellschaft` (
  `GESELLSCHAFTS_ID` int(11) NOT NULL,
  `GESELLSCHAFTSNAME` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `gutschrift`
--

CREATE TABLE `gutschrift` (
  `ID` int(50) NOT NULL,
  `NUMMER` varchar(50) NOT NULL,
  `MENGE` double NOT NULL,
  `PRODUKT_ID` int(50) NOT NULL,
  `MWST` int(50) NOT NULL,
  `PREIS2` double NOT NULL,
  `AUFTRAGGEBER` varchar(50) NOT NULL DEFAULT '',
  `DURCHFÜHRENDER` varchar(50) NOT NULL DEFAULT '',
  `FILIALE_ID` varchar(50) NOT NULL DEFAULT '',
  `RGESELLSCHAFTS_ID` int(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `inbetriebnahme`
--

CREATE TABLE `inbetriebnahme` (
  `INBETRIEBNAHME_ID` int(11) NOT NULL,
  `DATUMINBETRIEBNAHME` date DEFAULT NULL,
  `DATUMFUNKTIONSKONTROLLE` date DEFAULT NULL,
  `GERÄTE` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci COMMENT='Table ''demozentruminventardb.inbetriebnahme_copy'' doesn''t exist in engine';

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `invoice_access_tokens`
--

CREATE TABLE `invoice_access_tokens` (
  `id` bigint(20) NOT NULL,
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) DEFAULT NULL,
  `address_id` int(11) DEFAULT NULL,
  `token` varchar(160) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `expires_at` timestamp NULL DEFAULT NULL,
  `active` tinyint(1) DEFAULT 1,
  `access_count` int(11) DEFAULT 0,
  `last_access` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `kassenbuch`
--

CREATE TABLE `kassenbuch` (
  `ID` int(11) NOT NULL,
  `DATUM` date DEFAULT NULL,
  `GESCHÄFTSVORGANG` varchar(50) DEFAULT NULL,
  `STEUER` int(11) DEFAULT NULL,
  `EINNAHMEN` double DEFAULT NULL,
  `AUSGABEN` double DEFAULT NULL,
  `BESTAND` double DEFAULT NULL,
  `GEGENKONTO` varchar(50) DEFAULT NULL,
  `MANDANTENNUMMER` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `kassenbuchoben`
--

CREATE TABLE `kassenbuchoben` (
  `MANDANTENNUMMER` int(11) NOT NULL,
  `FIRMA` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `kontrolle`
--

CREATE TABLE `kontrolle` (
  `KONTROLL_ID` int(11) NOT NULL,
  `AUTORISIERTERPRÜFER` varchar(100) DEFAULT NULL,
  `GERÄTE` int(11) NOT NULL,
  `DATUMLETZTEPRÜFUNG_STK` date DEFAULT NULL,
  `INTERVALL_STK` int(11) DEFAULT NULL,
  `DATUMLETZTEPRÜFUNG_MTK` date DEFAULT NULL,
  `INTERVALL_MTK` int(11) DEFAULT NULL,
  `DATUMLETZTEPRÜFUNG_BGV_A3` date DEFAULT NULL,
  `INTERVALL_BGV_A3` int(11) DEFAULT NULL,
  `GEBRAUCHTANWEISUNG` bit(1) DEFAULT NULL,
  `MEDIZINPRODUKTEBUCH` bit(1) DEFAULT NULL,
  `INTERVALL` varchar(50) DEFAULT NULL,
  `AKTUELLE_PRÜFPLAKETTE` bit(1) DEFAULT NULL,
  `WARTUNGSVERTRAG` bit(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `kuerzel`
--

CREATE TABLE `kuerzel` (
  `ID` int(10) UNSIGNED NOT NULL,
  `KNAME` varchar(100) DEFAULT NULL,
  `KVORNAME` varchar(100) DEFAULT NULL,
  `KUERZEL` varchar(10) DEFAULT NULL,
  `ARZT` bit(1) DEFAULT NULL,
  `MITARBEITER` bit(1) DEFAULT NULL,
  `THERAPEUT` bit(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `lager`
--

CREATE TABLE `lager` (
  `CODE` int(11) NOT NULL,
  `BESCHREIBUNG` varchar(50) DEFAULT NULL,
  `LAGERORT` varchar(50) DEFAULT NULL,
  `MENGE` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `namenskonto`
--

CREATE TABLE `namenskonto` (
  `ID` int(11) NOT NULL,
  `konto` int(11) NOT NULL,
  `nname` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `news`
--

CREATE TABLE `news` (
  `ID` int(11) NOT NULL,
  `NEWS` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `ordnerfreigabe`
--

CREATE TABLE `ordnerfreigabe` (
  `ID` int(11) NOT NULL,
  `ORDNERFREIGABE` varchar(100) NOT NULL,
  `ARBEITSPLATZ_ID` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `ordnerfreigabeauswahl`
--

CREATE TABLE `ordnerfreigabeauswahl` (
  `ID` int(11) NOT NULL,
  `ORDNERFREIGABE` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `personal`
--

CREATE TABLE `personal` (
  `ID` int(11) NOT NULL,
  `NAME` varchar(50) DEFAULT NULL,
  `VORNAME` varchar(50) DEFAULT NULL,
  `STATUS` varchar(50) DEFAULT NULL,
  `POSITION` varchar(50) DEFAULT NULL,
  `FILIALE_ID` int(11) DEFAULT 0,
  `EMAIL` varchar(100) DEFAULT NULL,
  `TELEFON` varchar(100) DEFAULT NULL,
  `TELEFON2` varchar(100) DEFAULT NULL,
  `EMAIL_PW_EXTERN` varchar(50) DEFAULT NULL,
  `EMAIL_PW_INTERN` varchar(50) DEFAULT NULL,
  `RECHNER_IP` varchar(50) DEFAULT NULL,
  `OFFICE_LIZENZ` varchar(50) DEFAULT NULL,
  `PLONE_BENUTZER` varchar(50) DEFAULT NULL,
  `PLONE_PW` varchar(50) DEFAULT NULL,
  `MICROSOFT_KONTO` varchar(50) DEFAULT NULL,
  `MICROSOFT_PW` varchar(50) DEFAULT NULL,
  `DIENSTHANDY` bit(1) DEFAULT NULL,
  `VPN_TOKEN` bit(1) DEFAULT NULL,
  `VPN_PIN` varchar(50) DEFAULT NULL,
  `QNAP_BENUTZER` varchar(50) DEFAULT NULL,
  `QNAP_PW` varchar(50) DEFAULT NULL,
  `DIENSTLAPTOP` bit(1) DEFAULT NULL,
  `BEMERKUNG` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `preisliste`
--

CREATE TABLE `preisliste` (
  `ID` int(11) NOT NULL,
  `ARTIKEL` varchar(200) DEFAULT NULL,
  `LIEFERANT` varchar(200) DEFAULT NULL,
  `LETZTER_EINKAUFSPREIS` double DEFAULT NULL,
  `MONATLICHE_KOSTEN` double DEFAULT NULL,
  `BEMERKUNG` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `p_rechnung`
--

CREATE TABLE `p_rechnung` (
  `ID` int(50) NOT NULL,
  `PSRDID` int(11) DEFAULT NULL,
  `MENGE` double NOT NULL,
  `PRODUKT_ID` int(50) NOT NULL,
  `MWST` int(50) NOT NULL,
  `PREIS2` double NOT NULL,
  `RGESELLSCHAFTS_ID` int(50) NOT NULL,
  `AUFTRAGGEBER` varchar(50) DEFAULT NULL,
  `DURCHFÜHRENDER` varchar(50) DEFAULT NULL,
  `FILIALE_ID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `p_rechnungsdetails`
--

CREATE TABLE `p_rechnungsdetails` (
  `ID` int(50) NOT NULL,
  `PSRDID` int(50) DEFAULT NULL,
  `ADRESSID` int(50) DEFAULT NULL,
  `KINDADRESSID` int(50) DEFAULT NULL,
  `FIRMAADRESSID` int(50) DEFAULT NULL,
  `RDATUM` varchar(50) DEFAULT NULL,
  `BDATUM` varchar(50) DEFAULT NULL,
  `GPREIS` double DEFAULT NULL,
  `GBEMERKUNG` varchar(50) DEFAULT NULL,
  `RPROZENT` int(50) DEFAULT NULL,
  `RBEMERKUNG` varchar(50) DEFAULT NULL,
  `RGESELLSCHAFTS_ID` int(50) DEFAULT NULL,
  `GUTSCHRIFT` bit(1) DEFAULT NULL,
  `STORNO` bit(1) DEFAULT NULL,
  `ZAHLUNGSAVIS` bit(1) DEFAULT NULL,
  `ZAHLUNGSART` varchar(50) DEFAULT NULL,
  `RATENANZAHL` int(50) DEFAULT NULL,
  `ENDPREIS` double DEFAULT NULL,
  `USERNAME` varchar(50) DEFAULT NULL,
  `GRUND` varchar(50) DEFAULT NULL,
  `RFILIALE_ID` int(50) DEFAULT NULL,
  `DATEV` bit(1) DEFAULT NULL,
  `LOGO_PATH` varchar(500) DEFAULT NULL,
  `RECHNUNGSANREDE` varchar(500) DEFAULT NULL,
  `RECHNUNGSTEXT` varchar(500) DEFAULT NULL,
  `RECHNUNGSHINWEIS` varchar(500) DEFAULT NULL,
  `RECHNUNGSGRUSSFORMEL` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `p_rechnungsdummy`
--

CREATE TABLE `p_rechnungsdummy` (
  `rdaten_id` int(11) NOT NULL,
  `code` varchar(50) NOT NULL,
  `beschreibung` varchar(150) NOT NULL,
  `kategorie` varchar(50) DEFAULT NULL,
  `preis1` double DEFAULT NULL,
  `menge` double DEFAULT NULL,
  `mwst` int(50) DEFAULT NULL,
  `rgesellschafts_id` int(50) NOT NULL,
  `psrdid` varchar(50) NOT NULL,
  `auftraggeber` varchar(50) DEFAULT NULL,
  `durchführender` varchar(50) DEFAULT NULL,
  `filiale_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnung`
--

CREATE TABLE `rechnung` (
  `ID` int(50) NOT NULL,
  `NUMMER` varchar(50) NOT NULL,
  `MENGE` double NOT NULL,
  `PRODUKT_ID` int(50) NOT NULL,
  `MWST` int(50) DEFAULT NULL,
  `PREIS2` double NOT NULL,
  `RGESELLSCHAFTS_ID` int(50) NOT NULL,
  `AUFTRAGGEBER` varchar(50) DEFAULT NULL,
  `DURCHFÜHRENDER` varchar(50) DEFAULT NULL,
  `FILIALE_ID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungsanrede`
--

CREATE TABLE `rechnungsanrede` (
  `ID` int(11) NOT NULL,
  `TEXT` varchar(1000) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungsdaten`
--

CREATE TABLE `rechnungsdaten` (
  `rdaten_id` int(50) NOT NULL,
  `filiale_id` int(11) DEFAULT NULL,
  `code` varchar(200) DEFAULT NULL,
  `beschreibung` varchar(400) DEFAULT NULL,
  `abkürzung` varchar(50) DEFAULT NULL,
  `kategorie` varchar(50) DEFAULT NULL,
  `preis1` double DEFAULT NULL,
  `preisneu` double DEFAULT NULL,
  `preisalt` double DEFAULT NULL,
  `preis_gueltigab` date DEFAULT NULL,
  `mwst` int(50) DEFAULT NULL,
  `mwstalt` int(50) DEFAULT NULL,
  `mwst_gueltigab` date DEFAULT NULL,
  `konto` int(50) DEFAULT NULL,
  `rgesellschafts_id` int(50) DEFAULT NULL,
  `auftraggeber` varchar(50) DEFAULT NULL,
  `durchführender` varchar(50) DEFAULT NULL,
  `gültig_bis` date DEFAULT NULL,
  `gültig_ab` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungsdetails`
--

CREATE TABLE `rechnungsdetails` (
  `ID` int(50) NOT NULL,
  `RNUMMER` varchar(50) DEFAULT NULL,
  `ADRESSID` int(50) DEFAULT NULL,
  `KINDADRESSID` int(50) DEFAULT NULL,
  `FIRMAADRESSID` int(50) DEFAULT NULL,
  `RDATUM` varchar(50) DEFAULT NULL,
  `BDATUM` varchar(50) DEFAULT NULL,
  `GPREIS` double DEFAULT NULL,
  `GBEMERKUNG` varchar(200) DEFAULT NULL,
  `RPROZENT` int(50) DEFAULT NULL,
  `RBEMERKUNG` varchar(200) DEFAULT NULL,
  `RGESELLSCHAFTS_ID` int(50) DEFAULT NULL,
  `GUTSCHRIFT` bit(1) DEFAULT NULL,
  `STORNO` bit(1) DEFAULT NULL,
  `ZAHLUNGSAVIS` bit(1) DEFAULT NULL,
  `ZAHLUNGSART` varchar(50) DEFAULT NULL,
  `RATENANZAHL` int(50) DEFAULT NULL,
  `ENDPREIS` double DEFAULT NULL,
  `USERNAME` varchar(50) DEFAULT NULL,
  `GRUND` varchar(50) DEFAULT NULL,
  `RFILIALE_ID` int(50) DEFAULT NULL,
  `LOGO_PATH` varchar(500) DEFAULT NULL,
  `RECHNUNGSANREDE` varchar(500) DEFAULT NULL,
  `RECHNUNGSTEXT` varchar(500) DEFAULT NULL,
  `RECHNUNGSHINWEIS` varchar(500) DEFAULT NULL,
  `RECHNUNGSGRUSSFORMEL` varchar(500) DEFAULT NULL,
  `FADRESSE` bit(1) DEFAULT NULL,
  `FEMAIL` bit(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungsdummy`
--

CREATE TABLE `rechnungsdummy` (
  `rdaten_id` int(50) NOT NULL,
  `code` varchar(50) NOT NULL,
  `beschreibung` varchar(200) NOT NULL,
  `kategorie` varchar(50) DEFAULT NULL,
  `preis1` double DEFAULT NULL,
  `menge` double DEFAULT NULL,
  `mwst` int(50) DEFAULT NULL,
  `rgesellschafts_id` int(50) NOT NULL,
  `rnummer` varchar(50) NOT NULL,
  `auftraggeber` varchar(50) DEFAULT NULL,
  `durchführender` varchar(50) DEFAULT NULL,
  `filiale_id` int(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungsgesellschaft`
--

CREATE TABLE `rechnungsgesellschaft` (
  `id` int(50) NOT NULL,
  `gesellschaftskürzel` varchar(30) DEFAULT NULL,
  `gesellschaftsname` varchar(100) DEFAULT NULL,
  `gesellschaftsadresse` varchar(50) DEFAULT NULL,
  `post_straße_nummer` varchar(50) DEFAULT NULL,
  `post_plz_ort` varchar(50) DEFAULT NULL,
  `ustid` varchar(50) DEFAULT NULL,
  `register` varchar(50) DEFAULT NULL,
  `steuernummer` varchar(50) DEFAULT NULL,
  `gerichtsstand` varchar(50) DEFAULT NULL,
  `gesellschaftsführer` varchar(100) DEFAULT NULL,
  `ansprechpartner` varchar(50) DEFAULT NULL,
  `telefon` varchar(50) DEFAULT NULL,
  `fax` varchar(50) DEFAULT NULL,
  `email` varchar(50) DEFAULT NULL,
  `kontoinhaber` varchar(50) DEFAULT NULL,
  `iban` varchar(50) DEFAULT NULL,
  `bic` varchar(50) DEFAULT NULL,
  `neueskonto` bit(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungsgesellschaft_filiale`
--

CREATE TABLE `rechnungsgesellschaft_filiale` (
  `ID` int(11) NOT NULL,
  `RGESELLSCHAFTS_ID` int(11) DEFAULT NULL,
  `FILIALE_ID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungsgrussformel`
--

CREATE TABLE `rechnungsgrussformel` (
  `ID` int(11) NOT NULL,
  `TEXT` varchar(1000) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungslogo`
--

CREATE TABLE `rechnungslogo` (
  `ID` int(11) NOT NULL,
  `URL` varchar(1000) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungsrechtlicherhinweis`
--

CREATE TABLE `rechnungsrechtlicherhinweis` (
  `ID` int(11) NOT NULL,
  `TEXT` varchar(1000) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rechnungstext`
--

CREATE TABLE `rechnungstext` (
  `ID` int(11) NOT NULL,
  `TEXT` varchar(1000) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `replacement`
--

CREATE TABLE `replacement` (
  `ID` int(11) NOT NULL,
  `TEXT` varchar(1000) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `software`
--

CREATE TABLE `software` (
  `ID` int(11) NOT NULL,
  `SOFTWARE` varchar(100) NOT NULL,
  `ARBEITSPLATZ_ID` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `softwareauswahl`
--

CREATE TABLE `softwareauswahl` (
  `ID` int(11) NOT NULL,
  `SOFTWARE` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `storno`
--

CREATE TABLE `storno` (
  `ID` int(50) NOT NULL,
  `NUMMER` varchar(50) NOT NULL,
  `MENGE` double NOT NULL,
  `PRODUKT_ID` int(50) NOT NULL,
  `MWST` int(50) NOT NULL,
  `PREIS2` double NOT NULL,
  `AUFTRAGGEBER` varchar(50) DEFAULT NULL,
  `DURCHFÜHRENDER` varchar(50) DEFAULT NULL,
  `FILIALE_ID` int(11) DEFAULT NULL,
  `RGESELLSCHAFTS_ID` int(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `themes`
--

CREATE TABLE `themes` (
  `ID` int(11) NOT NULL,
  `NAME` varchar(50) NOT NULL,
  `THEMENAME` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `thetree`
--

CREATE TABLE `thetree` (
  `ID` int(11) NOT NULL,
  `NODE_NAME` varchar(50) DEFAULT NULL,
  `PARENT_ID` int(11) DEFAULT NULL,
  `APPLICATION_ID` int(11) DEFAULT NULL,
  `RGESELLSCHAFTS_ID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `translation_english`
--

CREATE TABLE `translation_english` (
  `ID` int(11) NOT NULL,
  `TRANSLATED_TEXT` varchar(500) DEFAULT NULL,
  `TRANSLATE_DESCRIPTION` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `translation_french`
--

CREATE TABLE `translation_french` (
  `ID` int(11) NOT NULL,
  `TRANSLATED_TEXT` varchar(500) DEFAULT NULL,
  `TRANSLATE_DESCRIPTION` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `translation_german`
--

CREATE TABLE `translation_german` (
  `ID` int(11) NOT NULL,
  `TRANSLATED_TEXT` varchar(500) DEFAULT NULL,
  `TRANSLATE_DESCRIPTION` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `translation_ukrainian`
--

CREATE TABLE `translation_ukrainian` (
  `ID` int(11) NOT NULL,
  `TRANSLATED_TEXT` varchar(500) DEFAULT NULL,
  `TRANSLATE_DESCRIPTION` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=DYNAMIC;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `userapplication`
--

CREATE TABLE `userapplication` (
  `ID` int(11) NOT NULL,
  `USERNAME` varchar(50) NOT NULL,
  `APPLICATION` varchar(50) NOT NULL,
  `RGESELLSCHAFTS_ID` int(11) DEFAULT NULL,
  `ROLE` varchar(50) DEFAULT 'user'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `verbrauchsmaterial`
--

CREATE TABLE `verbrauchsmaterial` (
  `ID` int(11) NOT NULL,
  `Name` varchar(200) DEFAULT NULL,
  `Eigenschaften` varchar(200) DEFAULT NULL,
  `Anzahl` int(11) DEFAULT NULL,
  `EMail_Hersteller` varchar(200) NOT NULL DEFAULT 'demo@example.local'
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `zahlungsavis`
--

CREATE TABLE `zahlungsavis` (
  `ID` int(50) NOT NULL,
  `NUMMER` varchar(50) NOT NULL,
  `MENGE` double NOT NULL,
  `PRODUKT_ID` int(50) NOT NULL,
  `MWST` int(50) NOT NULL,
  `PREIS2` double NOT NULL,
  `AUFTRAGGEBER` varchar(50) NOT NULL DEFAULT '',
  `DURCHFÜHRENDER` varchar(50) NOT NULL DEFAULT '',
  `FILIALE_ID` int(11) NOT NULL DEFAULT 0,
  `RGESELLSCHAFTS_ID` int(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
--


--
-- Indizes der exportierten Tabellen
--

--
-- Indizes für die Tabelle `accounts`
--
ALTER TABLE `accounts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `username` (`username`);

--
-- Indizes für die Tabelle `adressen`
--
ALTER TABLE `adressen`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `application`
--
ALTER TABLE `application`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `APPLICATION` (`APPLICATION`);

--
-- Indizes für die Tabelle `arbeitsplatz`
--
ALTER TABLE `arbeitsplatz`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_arbeitsplatz_filiale` (`FILIALE_ID`);

--
-- Indizes für die Tabelle `aufgaben`
--
ALTER TABLE `aufgaben`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_aufgaben_filiale` (`FILIALE_ID`);

--
-- Indizes für die Tabelle `einweisung`
--
ALTER TABLE `einweisung`
  ADD PRIMARY KEY (`EINWEISUNGS_ID`),
  ADD UNIQUE KEY `INBETRIEBNAHME_ID` (`INBETRIEBNAHME_ID`),
  ADD KEY `EinweisungsID` (`EINWEISUNGS_ID`);

--
-- Indizes für die Tabelle `filiale`
--
ALTER TABLE `filiale`
  ADD PRIMARY KEY (`FILIALE_ID`),
  ADD KEY `FK_filiale_gesellschaft` (`RGESELLSCHAFTS_ID`);

--
-- Indizes für die Tabelle `filiale_geräte_neu`
--
ALTER TABLE `filiale_geräte_neu`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_filiale_geräte_neu_filiale` (`rfiliale_ID`),
  ADD KEY `FK_filiale_geräte_neu_geräte_neu` (`geräte_neu_id`),
  ADD KEY `FK_filiale_geräte_neu_rechnungsgesellschaft` (`rgesellschafts_ID`);

--
-- Indizes für die Tabelle `freigabe`
--
ALTER TABLE `freigabe`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_freigabe_rechnungsgesellschaft` (`GESELLSCHAFT_ID`),
  ADD KEY `FK_freigabe_filiale` (`FILIALE_ID`);

--
-- Indizes für die Tabelle `gam_materialbewegung`
--
ALTER TABLE `gam_materialbewegung`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `IDX_gam_materialbewegung_material` (`MATERIAL_ID`),
  ADD KEY `IDX_gam_materialbewegung_geraet` (`GERAET_ID`);

--
-- Indizes für die Tabelle `geräte`
--
ALTER TABLE `geräte`
  ADD PRIMARY KEY (`GERÄTE_ID`),
  ADD KEY `FK_geräte_filiale` (`Filiale`);

--
-- Indizes für die Tabelle `geräte_neu`
--
ALTER TABLE `geräte_neu`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `geräte_neu_vmaterial`
--
ALTER TABLE `geräte_neu_vmaterial`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_geräte_neu_vmaterial_geräte_neu` (`GERÄTEID`),
  ADD KEY `FK_geräte_neu_vmaterial_verbrauchsmaterial` (`VMID`);

--
-- Indizes für die Tabelle `gesellschaft`
--
ALTER TABLE `gesellschaft`
  ADD PRIMARY KEY (`GESELLSCHAFTS_ID`),
  ADD KEY `GesellschaftsID` (`GESELLSCHAFTS_ID`);

--
-- Indizes für die Tabelle `gutschrift`
--
ALTER TABLE `gutschrift`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `inbetriebnahme`
--
ALTER TABLE `inbetriebnahme`
  ADD PRIMARY KEY (`INBETRIEBNAHME_ID`),
  ADD KEY `FK_inbetriebnahme_geräte` (`GERÄTE`);

--
-- Indizes für die Tabelle `invoice_access_tokens`
--
ALTER TABLE `invoice_access_tokens`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `token` (`token`),
  ADD KEY `idx_invoice_access_invoice` (`invoice_number`,`company_id`),
  ADD KEY `idx_invoice_access_address` (`address_id`),
  ADD KEY `idx_invoice_access_token` (`token`);

--
-- Indizes für die Tabelle `kassenbuch`
--
ALTER TABLE `kassenbuch`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_kassenbuch_kassenbuchoben` (`MANDANTENNUMMER`);

--
-- Indizes für die Tabelle `kassenbuchoben`
--
ALTER TABLE `kassenbuchoben`
  ADD PRIMARY KEY (`MANDANTENNUMMER`);

--
-- Indizes für die Tabelle `kontrolle`
--
ALTER TABLE `kontrolle`
  ADD PRIMARY KEY (`KONTROLL_ID`),
  ADD KEY `KontrollID` (`KONTROLL_ID`),
  ADD KEY `FK_kontrolle_geräte` (`GERÄTE`);

--
-- Indizes für die Tabelle `kuerzel`
--
ALTER TABLE `kuerzel`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `lager`
--
ALTER TABLE `lager`
  ADD PRIMARY KEY (`CODE`);

--
-- Indizes für die Tabelle `namenskonto`
--
ALTER TABLE `namenskonto`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `news`
--
ALTER TABLE `news`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `ordnerfreigabe`
--
ALTER TABLE `ordnerfreigabe`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_ordnerfreigabe_arbeitsplatz` (`ARBEITSPLATZ_ID`);

--
-- Indizes für die Tabelle `ordnerfreigabeauswahl`
--
ALTER TABLE `ordnerfreigabeauswahl`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `personal`
--
ALTER TABLE `personal`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_personal_filiale` (`FILIALE_ID`);

--
-- Indizes für die Tabelle `preisliste`
--
ALTER TABLE `preisliste`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `p_rechnung`
--
ALTER TABLE `p_rechnung`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_rechnung_rechnungsdaten` (`PRODUKT_ID`),
  ADD KEY `FK_rechnung_rechnungsgesellschaft` (`RGESELLSCHAFTS_ID`),
  ADD KEY `NUMMER` (`PSRDID`);

--
-- Indizes für die Tabelle `p_rechnungsdetails`
--
ALTER TABLE `p_rechnungsdetails`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_rechnungsdetails_adressen` (`ADRESSID`),
  ADD KEY `FK_rechnungsdetails_rechnungsgesellschaft` (`RGESELLSCHAFTS_ID`),
  ADD KEY `FK_rechnungsdetails_adressen_2` (`KINDADRESSID`);

--
-- Indizes für die Tabelle `p_rechnungsdummy`
--
ALTER TABLE `p_rechnungsdummy`
  ADD PRIMARY KEY (`rdaten_id`),
  ADD KEY `FK_rechnungsdaten_rechnungsgesellschaft` (`rgesellschafts_id`),
  ADD KEY `FK_rechnungsdummy_rechnung` (`psrdid`);

--
-- Indizes für die Tabelle `rechnung`
--
ALTER TABLE `rechnung`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `NUMMER` (`NUMMER`),
  ADD KEY `FK_rechnung_rechnungsdaten` (`PRODUKT_ID`),
  ADD KEY `FK_rechnung_rechnungsgesellschaft` (`RGESELLSCHAFTS_ID`),
  ADD KEY `FK_rechnung_filiale` (`FILIALE_ID`);

--
-- Indizes für die Tabelle `rechnungsanrede`
--
ALTER TABLE `rechnungsanrede`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `rechnungsdaten`
--
ALTER TABLE `rechnungsdaten`
  ADD PRIMARY KEY (`rdaten_id`),
  ADD KEY `FK_rechnungsdaten_rechnungsgesellschaft` (`rgesellschafts_id`);

--
-- Indizes für die Tabelle `rechnungsdetails`
--
ALTER TABLE `rechnungsdetails`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_rechnungsdetails_adressen` (`ADRESSID`),
  ADD KEY `FK_rechnungsdetails_rechnung` (`RNUMMER`),
  ADD KEY `FK_rechnungsdetails_rechnungsgesellschaft` (`RGESELLSCHAFTS_ID`),
  ADD KEY `FK_rechnungsdetails_adressen_2` (`KINDADRESSID`);

--
-- Indizes für die Tabelle `rechnungsdummy`
--
ALTER TABLE `rechnungsdummy`
  ADD PRIMARY KEY (`rdaten_id`),
  ADD KEY `FK_rechnungsdaten_rechnungsgesellschaft` (`rgesellschafts_id`),
  ADD KEY `FK_rechnungsdummy_rechnung` (`rnummer`);

--
-- Indizes für die Tabelle `rechnungsgesellschaft`
--
ALTER TABLE `rechnungsgesellschaft`
  ADD PRIMARY KEY (`id`);

--
-- Indizes für die Tabelle `rechnungsgesellschaft_filiale`
--
ALTER TABLE `rechnungsgesellschaft_filiale`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_rechnungsgesellschaft_filiale_rechnungsgesellschaft` (`RGESELLSCHAFTS_ID`),
  ADD KEY `FK_rechnungsgesellschaft_filiale_filiale` (`FILIALE_ID`);

--
-- Indizes für die Tabelle `rechnungsgrussformel`
--
ALTER TABLE `rechnungsgrussformel`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `rechnungslogo`
--
ALTER TABLE `rechnungslogo`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `rechnungsrechtlicherhinweis`
--
ALTER TABLE `rechnungsrechtlicherhinweis`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `rechnungstext`
--
ALTER TABLE `rechnungstext`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `replacement`
--
ALTER TABLE `replacement`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `software`
--
ALTER TABLE `software`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_software_arbeitsplatz` (`ARBEITSPLATZ_ID`);

--
-- Indizes für die Tabelle `softwareauswahl`
--
ALTER TABLE `softwareauswahl`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `storno`
--
ALTER TABLE `storno`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_storno_filiale` (`FILIALE_ID`);

--
-- Indizes für die Tabelle `themes`
--
ALTER TABLE `themes`
  ADD PRIMARY KEY (`ID`);

--
-- Indizes für die Tabelle `thetree`
--
ALTER TABLE `thetree`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_thetree_application` (`APPLICATION_ID`);

--
-- Indizes für die Tabelle `translation_english`
--
ALTER TABLE `translation_english`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `translation_french`
--
ALTER TABLE `translation_french`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `translation_german`
--
ALTER TABLE `translation_german`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `translation_ukrainian`
--
ALTER TABLE `translation_ukrainian`
  ADD PRIMARY KEY (`ID`) USING BTREE;

--
-- Indizes für die Tabelle `userapplication`
--
ALTER TABLE `userapplication`
  ADD PRIMARY KEY (`ID`),
  ADD KEY `FK_userapplication_accounts` (`USERNAME`),
  ADD KEY `FK_userapplication_application` (`APPLICATION`),
  ADD KEY `FK_userapplication_rechnungsgesellschaft` (`RGESELLSCHAFTS_ID`);

--
-- Indizes für die Tabelle `verbrauchsmaterial`
--
ALTER TABLE `verbrauchsmaterial`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `Name` (`Name`);

--
-- Indizes für die Tabelle `zahlungsavis`
--
ALTER TABLE `zahlungsavis`
  ADD PRIMARY KEY (`ID`);

--
-- AUTO_INCREMENT für exportierte Tabellen
--

--
-- AUTO_INCREMENT für Tabelle `accounts`
--
ALTER TABLE `accounts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=110;

--
-- AUTO_INCREMENT für Tabelle `adressen`
--
ALTER TABLE `adressen`
  MODIFY `ID` int(100) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=37754;

--
-- AUTO_INCREMENT für Tabelle `application`
--
ALTER TABLE `application`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT für Tabelle `arbeitsplatz`
--
ALTER TABLE `arbeitsplatz`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `aufgaben`
--
ALTER TABLE `aufgaben`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1951;

--
-- AUTO_INCREMENT für Tabelle `einweisung`
--
ALTER TABLE `einweisung`
  MODIFY `EINWEISUNGS_ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=432;

--
-- AUTO_INCREMENT für Tabelle `filiale`
--
ALTER TABLE `filiale`
  MODIFY `FILIALE_ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=83;

--
-- AUTO_INCREMENT für Tabelle `filiale_geräte_neu`
--
ALTER TABLE `filiale_geräte_neu`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=72;

--
-- AUTO_INCREMENT für Tabelle `freigabe`
--
ALTER TABLE `freigabe`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=34;

--
-- AUTO_INCREMENT für Tabelle `gam_materialbewegung`
--
ALTER TABLE `gam_materialbewegung`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `geräte`
--
ALTER TABLE `geräte`
  MODIFY `GERÄTE_ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=426;

--
-- AUTO_INCREMENT für Tabelle `geräte_neu`
--
ALTER TABLE `geräte_neu`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=61;

--
-- AUTO_INCREMENT für Tabelle `geräte_neu_vmaterial`
--
ALTER TABLE `geräte_neu_vmaterial`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=115;

--
-- AUTO_INCREMENT für Tabelle `gesellschaft`
--
ALTER TABLE `gesellschaft`
  MODIFY `GESELLSCHAFTS_ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=80;

--
-- AUTO_INCREMENT für Tabelle `gutschrift`
--
ALTER TABLE `gutschrift`
  MODIFY `ID` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT für Tabelle `inbetriebnahme`
--
ALTER TABLE `inbetriebnahme`
  MODIFY `INBETRIEBNAHME_ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=335;

--
-- AUTO_INCREMENT für Tabelle `invoice_access_tokens`
--
ALTER TABLE `invoice_access_tokens`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT für Tabelle `kassenbuch`
--
ALTER TABLE `kassenbuch`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=22;

--
-- AUTO_INCREMENT für Tabelle `kassenbuchoben`
--
ALTER TABLE `kassenbuchoben`
  MODIFY `MANDANTENNUMMER` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT für Tabelle `kontrolle`
--
ALTER TABLE `kontrolle`
  MODIFY `KONTROLL_ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=395;

--
-- AUTO_INCREMENT für Tabelle `kuerzel`
--
ALTER TABLE `kuerzel`
  MODIFY `ID` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=790;

--
-- AUTO_INCREMENT für Tabelle `lager`
--
ALTER TABLE `lager`
  MODIFY `CODE` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT für Tabelle `namenskonto`
--
ALTER TABLE `namenskonto`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=31;

--
-- AUTO_INCREMENT für Tabelle `news`
--
ALTER TABLE `news`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT für Tabelle `ordnerfreigabe`
--
ALTER TABLE `ordnerfreigabe`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `ordnerfreigabeauswahl`
--
ALTER TABLE `ordnerfreigabeauswahl`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT für Tabelle `personal`
--
ALTER TABLE `personal`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=524;

--
-- AUTO_INCREMENT für Tabelle `preisliste`
--
ALTER TABLE `preisliste`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=70;

--
-- AUTO_INCREMENT für Tabelle `p_rechnung`
--
ALTER TABLE `p_rechnung`
  MODIFY `ID` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT für Tabelle `p_rechnungsdetails`
--
ALTER TABLE `p_rechnungsdetails`
  MODIFY `ID` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT für Tabelle `p_rechnungsdummy`
--
ALTER TABLE `p_rechnungsdummy`
  MODIFY `rdaten_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `rechnung`
--
ALTER TABLE `rechnung`
  MODIFY `ID` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62903;

--
-- AUTO_INCREMENT für Tabelle `rechnungsanrede`
--
ALTER TABLE `rechnungsanrede`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT für Tabelle `rechnungsdaten`
--
ALTER TABLE `rechnungsdaten`
  MODIFY `rdaten_id` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1685;

--
-- AUTO_INCREMENT für Tabelle `rechnungsdetails`
--
ALTER TABLE `rechnungsdetails`
  MODIFY `ID` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=54026;

--
-- AUTO_INCREMENT für Tabelle `rechnungsdummy`
--
ALTER TABLE `rechnungsdummy`
  MODIFY `rdaten_id` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4058;

--
-- AUTO_INCREMENT für Tabelle `rechnungsgesellschaft`
--
ALTER TABLE `rechnungsgesellschaft`
  MODIFY `id` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT für Tabelle `rechnungsgesellschaft_filiale`
--
ALTER TABLE `rechnungsgesellschaft_filiale`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

--
-- AUTO_INCREMENT für Tabelle `rechnungsgrussformel`
--
ALTER TABLE `rechnungsgrussformel`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT für Tabelle `rechnungslogo`
--
ALTER TABLE `rechnungslogo`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT für Tabelle `rechnungsrechtlicherhinweis`
--
ALTER TABLE `rechnungsrechtlicherhinweis`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `rechnungstext`
--
ALTER TABLE `rechnungstext`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT für Tabelle `replacement`
--
ALTER TABLE `replacement`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT für Tabelle `software`
--
ALTER TABLE `software`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT für Tabelle `softwareauswahl`
--
ALTER TABLE `softwareauswahl`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT für Tabelle `storno`
--
ALTER TABLE `storno`
  MODIFY `ID` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3327;

--
-- AUTO_INCREMENT für Tabelle `themes`
--
ALTER TABLE `themes`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=40;

--
-- AUTO_INCREMENT für Tabelle `thetree`
--
ALTER TABLE `thetree`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT für Tabelle `translation_english`
--
ALTER TABLE `translation_english`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=342;

--
-- AUTO_INCREMENT für Tabelle `translation_french`
--
ALTER TABLE `translation_french`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=328;

--
-- AUTO_INCREMENT für Tabelle `translation_german`
--
ALTER TABLE `translation_german`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=390;

--
-- AUTO_INCREMENT für Tabelle `translation_ukrainian`
--
ALTER TABLE `translation_ukrainian`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=307;

--
-- AUTO_INCREMENT für Tabelle `userapplication`
--
ALTER TABLE `userapplication`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=684;

--
-- AUTO_INCREMENT für Tabelle `verbrauchsmaterial`
--
ALTER TABLE `verbrauchsmaterial`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=47;

--
-- AUTO_INCREMENT für Tabelle `zahlungsavis`
--
ALTER TABLE `zahlungsavis`
  MODIFY `ID` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=174;

--
-- Constraints der exportierten Tabellen
--

--
-- Constraints der Tabelle `arbeitsplatz`
--
ALTER TABLE `arbeitsplatz`
  ADD CONSTRAINT `FK_arbeitsplatz_filiale` FOREIGN KEY (`FILIALE_ID`) REFERENCES `filiale` (`FILIALE_ID`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints der Tabelle `aufgaben`
--
ALTER TABLE `aufgaben`
  ADD CONSTRAINT `FK_aufgaben_filiale` FOREIGN KEY (`FILIALE_ID`) REFERENCES `filiale` (`FILIALE_ID`);

--
-- Constraints der Tabelle `filiale`
--
ALTER TABLE `filiale`
  ADD CONSTRAINT `FK_filiale_gesellschaft` FOREIGN KEY (`RGESELLSCHAFTS_ID`) REFERENCES `gesellschaft` (`GESELLSCHAFTS_ID`);

--
-- Constraints der Tabelle `filiale_geräte_neu`
--
ALTER TABLE `filiale_geräte_neu`
  ADD CONSTRAINT `FK_filiale_geräte_neu_filiale` FOREIGN KEY (`rfiliale_ID`) REFERENCES `filiale` (`FILIALE_ID`),
  ADD CONSTRAINT `FK_filiale_geräte_neu_geräte_neu` FOREIGN KEY (`geräte_neu_id`) REFERENCES `geräte_neu` (`ID`),
  ADD CONSTRAINT `FK_filiale_geräte_neu_rechnungsgesellschaft` FOREIGN KEY (`rgesellschafts_ID`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `freigabe`
--
ALTER TABLE `freigabe`
  ADD CONSTRAINT `FK_freigabe_filiale` FOREIGN KEY (`FILIALE_ID`) REFERENCES `filiale` (`FILIALE_ID`);

--
-- Constraints der Tabelle `geräte`
--
ALTER TABLE `geräte`
  ADD CONSTRAINT `FK_geräte_filiale` FOREIGN KEY (`Filiale`) REFERENCES `filiale` (`FILIALE_ID`);

--
-- Constraints der Tabelle `geräte_neu_vmaterial`
--
ALTER TABLE `geräte_neu_vmaterial`
  ADD CONSTRAINT `FK_geräte_neu_vmaterial_geräte_neu` FOREIGN KEY (`GERÄTEID`) REFERENCES `geräte_neu` (`ID`),
  ADD CONSTRAINT `FK_geräte_neu_vmaterial_verbrauchsmaterial` FOREIGN KEY (`VMID`) REFERENCES `verbrauchsmaterial` (`ID`);

--
-- Constraints der Tabelle `inbetriebnahme`
--
ALTER TABLE `inbetriebnahme`
  ADD CONSTRAINT `FK_inbetriebnahme_geräte` FOREIGN KEY (`GERÄTE`) REFERENCES `geräte` (`GERÄTE_ID`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints der Tabelle `kassenbuch`
--
ALTER TABLE `kassenbuch`
  ADD CONSTRAINT `FK_kassenbuch_kassenbuchoben` FOREIGN KEY (`MANDANTENNUMMER`) REFERENCES `kassenbuchoben` (`MANDANTENNUMMER`);

--
-- Constraints der Tabelle `kontrolle`
--
ALTER TABLE `kontrolle`
  ADD CONSTRAINT `FK_kontrolle_geräte` FOREIGN KEY (`GERÄTE`) REFERENCES `geräte` (`GERÄTE_ID`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints der Tabelle `ordnerfreigabe`
--
ALTER TABLE `ordnerfreigabe`
  ADD CONSTRAINT `FK_ordnerfreigabe_arbeitsplatz` FOREIGN KEY (`ARBEITSPLATZ_ID`) REFERENCES `arbeitsplatz` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints der Tabelle `personal`
--
ALTER TABLE `personal`
  ADD CONSTRAINT `FK_personal_filiale` FOREIGN KEY (`FILIALE_ID`) REFERENCES `filiale` (`FILIALE_ID`);

--
-- Constraints der Tabelle `p_rechnung`
--
ALTER TABLE `p_rechnung`
  ADD CONSTRAINT `FK_p_rechnung_p_rechnungsdetails` FOREIGN KEY (`PSRDID`) REFERENCES `p_rechnungsdetails` (`ID`),
  ADD CONSTRAINT `FK_p_rechnung_rechnungsdaten` FOREIGN KEY (`PRODUKT_ID`) REFERENCES `rechnungsdaten` (`rdaten_id`),
  ADD CONSTRAINT `p_rechnung_ibfk_1` FOREIGN KEY (`RGESELLSCHAFTS_ID`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `p_rechnungsdetails`
--
ALTER TABLE `p_rechnungsdetails`
  ADD CONSTRAINT `p_rechnungsdetails_ibfk_1` FOREIGN KEY (`ADRESSID`) REFERENCES `adressen` (`ID`),
  ADD CONSTRAINT `p_rechnungsdetails_ibfk_2` FOREIGN KEY (`KINDADRESSID`) REFERENCES `adressen` (`ID`),
  ADD CONSTRAINT `p_rechnungsdetails_ibfk_3` FOREIGN KEY (`RGESELLSCHAFTS_ID`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `p_rechnungsdummy`
--
ALTER TABLE `p_rechnungsdummy`
  ADD CONSTRAINT `p_rechnungsdummy_ibfk_1` FOREIGN KEY (`rgesellschafts_id`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `rechnung`
--
ALTER TABLE `rechnung`
  ADD CONSTRAINT `FK_rechnung_filiale` FOREIGN KEY (`FILIALE_ID`) REFERENCES `filiale` (`FILIALE_ID`),
  ADD CONSTRAINT `FK_rechnung_rechnungsdaten` FOREIGN KEY (`PRODUKT_ID`) REFERENCES `rechnungsdaten` (`rdaten_id`),
  ADD CONSTRAINT `FK_rechnung_rechnungsgesellschaft` FOREIGN KEY (`RGESELLSCHAFTS_ID`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `rechnungsdaten`
--
ALTER TABLE `rechnungsdaten`
  ADD CONSTRAINT `FK_rechnungsdaten_rechnungsgesellschaft` FOREIGN KEY (`rgesellschafts_id`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `rechnungsdetails`
--
ALTER TABLE `rechnungsdetails`
  ADD CONSTRAINT `FK_rechnungsdetails_adressen` FOREIGN KEY (`ADRESSID`) REFERENCES `adressen` (`ID`),
  ADD CONSTRAINT `FK_rechnungsdetails_adressen_2` FOREIGN KEY (`KINDADRESSID`) REFERENCES `adressen` (`ID`),
  ADD CONSTRAINT `FK_rechnungsdetails_rechnungsgesellschaft` FOREIGN KEY (`RGESELLSCHAFTS_ID`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `rechnungsdummy`
--
ALTER TABLE `rechnungsdummy`
  ADD CONSTRAINT `FK_rechnungsdummy_rechnungsgesellschaft` FOREIGN KEY (`rgesellschafts_id`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `rechnungsgesellschaft_filiale`
--
ALTER TABLE `rechnungsgesellschaft_filiale`
  ADD CONSTRAINT `FK_rechnungsgesellschaft_filiale_filiale` FOREIGN KEY (`FILIALE_ID`) REFERENCES `filiale` (`FILIALE_ID`),
  ADD CONSTRAINT `FK_rechnungsgesellschaft_filiale_rechnungsgesellschaft` FOREIGN KEY (`RGESELLSCHAFTS_ID`) REFERENCES `rechnungsgesellschaft` (`id`);

--
-- Constraints der Tabelle `software`
--
ALTER TABLE `software`
  ADD CONSTRAINT `FK_software_arbeitsplatz` FOREIGN KEY (`ARBEITSPLATZ_ID`) REFERENCES `arbeitsplatz` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints der Tabelle `storno`
--
ALTER TABLE `storno`
  ADD CONSTRAINT `FK_storno_filiale` FOREIGN KEY (`FILIALE_ID`) REFERENCES `filiale` (`FILIALE_ID`);

--
-- Constraints der Tabelle `thetree`
--
ALTER TABLE `thetree`
  ADD CONSTRAINT `FK_thetree_application` FOREIGN KEY (`APPLICATION_ID`) REFERENCES `application` (`ID`);

--
-- Constraints der Tabelle `userapplication`
--
ALTER TABLE `userapplication`
  ADD CONSTRAINT `FK_userapplication_accounts` FOREIGN KEY (`USERNAME`) REFERENCES `accounts` (`username`),
  ADD CONSTRAINT `FK_userapplication_application` FOREIGN KEY (`APPLICATION`) REFERENCES `application` (`APPLICATION`),
  ADD CONSTRAINT `FK_userapplication_rechnungsgesellschaft` FOREIGN KEY (`RGESELLSCHAFTS_ID`) REFERENCES `rechnungsgesellschaft` (`id`);

-- ============================================================
-- GAM 2.1.0 Preview 2 – neue Workflow-Module
-- ============================================================

CREATE TABLE IF NOT EXISTS `laboratory_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `patient_number` VARCHAR(80) DEFAULT NULL,
  `patient_name` VARCHAR(255) NOT NULL,
  `requested_by` VARCHAR(255) DEFAULT NULL,
  `external_laboratory` VARCHAR(255) DEFAULT NULL,
  `examinations` TEXT DEFAULT NULL,
  `specimen_material` VARCHAR(120) DEFAULT NULL,
  `priority` VARCHAR(40) NOT NULL DEFAULT 'NORMAL',
  `status` VARCHAR(60) NOT NULL DEFAULT 'ANGELEGT',
  `due_date` DATE DEFAULT NULL,
  `collected_at` DATETIME DEFAULT NULL,
  `collected_by` VARCHAR(255) DEFAULT NULL,
  `specimen_id` VARCHAR(120) DEFAULT NULL,
  `sent_at` DATETIME DEFAULT NULL,
  `result_received_at` DATETIME DEFAULT NULL,
  `result_summary` TEXT DEFAULT NULL,
  `reviewed_by` VARCHAR(255) DEFAULT NULL,
  `reviewed_at` DATETIME DEFAULT NULL,
  `patient_information` TEXT DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `created_by` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_laboratory_status` (`status`), KEY `idx_laboratory_due` (`due_date`), KEY `idx_laboratory_patient` (`patient_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `waiting_room_visit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `patient_number` VARCHAR(80) DEFAULT NULL,
  `patient_name` VARCHAR(255) NOT NULL,
  `appointment_type` VARCHAR(160) DEFAULT NULL,
  `practitioner` VARCHAR(255) DEFAULT NULL,
  `room` VARCHAR(100) DEFAULT NULL,
  `priority` VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
  `status` VARCHAR(40) NOT NULL DEFAULT 'ANGEKUENDIGT',
  `appointment_at` DATETIME DEFAULT NULL,
  `arrived_at` DATETIME DEFAULT NULL,
  `called_at` DATETIME DEFAULT NULL,
  `treatment_started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `next_step` VARCHAR(120) DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `created_by` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_wait_status` (`status`), KEY `idx_wait_appointment` (`appointment_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;


-- ============================================================
-- GAM 2.0 v1.8.0 Demo-Erweiterungen
-- Stand: 2026-06-30
-- Zweck: anonymisierte, direkt testbare Datenbasis für die neuen
-- Verwaltungsbereiche v1.8.0 inkl. anonymisierter Gesellschaften.
-- ============================================================

START TRANSACTION;

-- Demo-Loginbenutzer: admin/admin, mainuser/mainuser, user/user
DELETE FROM `accounts` WHERE `username` IN ('admin','administrator','mainuser','user','personal','itadmin');

-- Anonymisierte Gesellschaften für Demo und Tests
DELETE FROM `gesellschaft` WHERE `GESELLSCHAFTS_ID` BETWEEN 900 AND 999;

-- Anonymisierte Filialen / Standorte
DELETE FROM `filiale` WHERE `FILIALE_ID` BETWEEN 900 AND 999;

-- Personal: neue HR-Bemerkungsspalte für 38l3+ falls noch nicht vorhanden
ALTER TABLE `personal` ADD COLUMN IF NOT EXISTS `BEMERKUNG_PERSONAL` varchar(200) DEFAULT NULL AFTER `TELEFON2`;
DELETE FROM `personal` WHERE `ID` BETWEEN 9000 AND 9010;

-- Demo-Rechte für neue v1.8.0-Module
DELETE FROM `userapplication` WHERE `USERNAME` IN ('admin','mainuser','user','personal','itadmin');

-- Arbeitsplatzausstattung
DELETE FROM `arbeitsplatz` WHERE `ID` BETWEEN 9000 AND 9010;

-- Preisliste
DELETE FROM `preisliste` WHERE `ID` BETWEEN 9000 AND 9010;

-- Kassenbuch
DELETE FROM `kassenbuch` WHERE `ID` BETWEEN 9000 AND 9010;

-- Geräte für Prüfungsfristen
DELETE FROM `geräte` WHERE `GERÄTE_ID` BETWEEN 9000 AND 9010;

-- Prüfungen / Inbetriebnahmen / Einweisungen
DELETE FROM `kontrolle` WHERE `KONTROLL_ID` BETWEEN 9000 AND 9010;

DELETE FROM `inbetriebnahme` WHERE `INBETRIEBNAHME_ID` BETWEEN 9000 AND 9010;

DELETE FROM `einweisung` WHERE `EINWEISUNGS_ID` BETWEEN 9000 AND 9010;


-- ============================================================
-- GAM 2.1.0 Preview 2 – neue Workflow-Module
-- ============================================================

CREATE TABLE IF NOT EXISTS `laboratory_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `patient_number` VARCHAR(80) DEFAULT NULL,
  `patient_name` VARCHAR(255) NOT NULL,
  `requested_by` VARCHAR(255) DEFAULT NULL,
  `external_laboratory` VARCHAR(255) DEFAULT NULL,
  `examinations` TEXT DEFAULT NULL,
  `specimen_material` VARCHAR(120) DEFAULT NULL,
  `priority` VARCHAR(40) NOT NULL DEFAULT 'NORMAL',
  `status` VARCHAR(60) NOT NULL DEFAULT 'ANGELEGT',
  `due_date` DATE DEFAULT NULL,
  `collected_at` DATETIME DEFAULT NULL,
  `collected_by` VARCHAR(255) DEFAULT NULL,
  `specimen_id` VARCHAR(120) DEFAULT NULL,
  `sent_at` DATETIME DEFAULT NULL,
  `result_received_at` DATETIME DEFAULT NULL,
  `result_summary` TEXT DEFAULT NULL,
  `reviewed_by` VARCHAR(255) DEFAULT NULL,
  `reviewed_at` DATETIME DEFAULT NULL,
  `patient_information` TEXT DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `created_by` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_laboratory_status` (`status`), KEY `idx_laboratory_due` (`due_date`), KEY `idx_laboratory_patient` (`patient_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `waiting_room_visit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `patient_number` VARCHAR(80) DEFAULT NULL,
  `patient_name` VARCHAR(255) NOT NULL,
  `appointment_type` VARCHAR(160) DEFAULT NULL,
  `practitioner` VARCHAR(255) DEFAULT NULL,
  `room` VARCHAR(100) DEFAULT NULL,
  `priority` VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
  `status` VARCHAR(40) NOT NULL DEFAULT 'ANGEKUENDIGT',
  `appointment_at` DATETIME DEFAULT NULL,
  `arrived_at` DATETIME DEFAULT NULL,
  `called_at` DATETIME DEFAULT NULL,
  `treatment_started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `next_step` VARCHAR(120) DEFAULT NULL,
  `note` TEXT DEFAULT NULL,
  `created_by` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_wait_status` (`status`), KEY `idx_wait_appointment` (`appointment_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



COMMIT;

-- Ende GAM 2.0 v1.8.0 anonymisierte Demo-Erweiterungen


-- GAM 40k33a10 RUNTIME SCHEMA SUPPLEMENT
-- ============================================================
-- GAM 2.1.0 Preview 2 / Schritt 40k33a10
-- Vollstaendige additive Laufzeit- und Modultabellen
-- Quelle der Tabellenstruktur: exportierte funktionierende GAM-Datenbank
-- Enthält KEINE Bestands-, Patienten-, Konto- oder Praxisdaten.
-- Kann unter Linux direkt in kopfzentruminventardb importiert werden.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;
CREATE TABLE IF NOT EXISTS `account_passkeys` (
  `id` bigint(20) NOT NULL,
  `account_id` int(11) NOT NULL,
  `credential_id` varchar(512) NOT NULL,
  `public_key` longtext DEFAULT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `sign_count` bigint(20) NOT NULL DEFAULT 0,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_used_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `communication_workflow_item` (
  `id` bigint(20) NOT NULL,
  `channel` varchar(30) NOT NULL DEFAULT 'E-Mail',
  `direction` varchar(20) NOT NULL DEFAULT 'EINGANG',
  `sender` varchar(255) DEFAULT NULL,
  `recipient` varchar(255) DEFAULT NULL,
  `subject` varchar(500) NOT NULL,
  `message` text DEFAULT NULL,
  `responsible` varchar(120) DEFAULT NULL,
  `priority` varchar(20) NOT NULL DEFAULT 'mittel',
  `status` varchar(30) NOT NULL DEFAULT 'NEU',
  `due_date` date DEFAULT NULL,
  `result_note` text DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `communication_workflow_settings` (
  `setting_key` varchar(120) NOT NULL,
  `setting_value` varchar(255) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_discovery_builtin_sources` (
  `source_key` varchar(80) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_discovery_device_type_history` (
  `id` bigint(20) NOT NULL,
  `identity_key` varchar(255) NOT NULL,
  `old_device_type` varchar(255) DEFAULT NULL,
  `new_device_type` varchar(255) NOT NULL,
  `changed_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `changed_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_discovery_merge_settings` (
  `id` int(11) NOT NULL,
  `auto_merge_threshold` int(11) NOT NULL DEFAULT 95,
  `duplicate_threshold` int(11) NOT NULL DEFAULT 60,
  `mac_weight` int(11) NOT NULL DEFAULT 85,
  `hardware_serial_weight` int(11) NOT NULL DEFAULT 95,
  `snmp_serial_weight` int(11) NOT NULL DEFAULT 90,
  `device_id_weight` int(11) NOT NULL DEFAULT 80,
  `hostname_weight` int(11) NOT NULL DEFAULT 50,
  `ip_weight` int(11) NOT NULL DEFAULT 5,
  `manufacturer_weight` int(11) NOT NULL DEFAULT 10,
  `type_weight` int(11) NOT NULL DEFAULT 15,
  `two_source_bonus` int(11) NOT NULL DEFAULT 5,
  `three_source_bonus` int(11) NOT NULL DEFAULT 10,
  `automatic_merge_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `hard_conflicts_block_merge` tinyint(1) NOT NULL DEFAULT 1,
  `ip_never_merges_alone` tinyint(1) NOT NULL DEFAULT 1,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_discovery_registered_devices` (
  `identity_key` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `hardware_address` varchar(100) DEFAULT NULL,
  `serial_number` varchar(255) DEFAULT NULL,
  `registered_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `device_type` varchar(255) DEFAULT NULL,
  `manufacturer` varchar(255) DEFAULT NULL,
  `discovery_protocol` longtext DEFAULT NULL,
  `device_status` varchar(100) DEFAULT NULL,
  `first_seen_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_seen_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `detection_count` bigint(20) NOT NULL DEFAULT 0,
  `last_scan_hits` int(11) NOT NULL DEFAULT 0,
  `manual_device_type` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_fritzbox_sources` (
  `id` bigint(20) NOT NULL,
  `name` varchar(120) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `host` varchar(255) NOT NULL,
  `port` int(11) NOT NULL DEFAULT 49000,
  `username` varchar(255) DEFAULT NULL,
  `password` text DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_homeassistant_sources` (
  `id` bigint(20) NOT NULL,
  `name` varchar(120) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `base_url` varchar(500) NOT NULL,
  `access_token` text DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_marketing_action_type` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 100
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_marketing_campaign` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `material_name` varchar(255) NOT NULL,
  `material_code` varchar(120) DEFAULT NULL,
  `source_warehouse` varchar(255) DEFAULT NULL,
  `target_branch` varchar(255) NOT NULL,
  `planned_quantity` int(11) NOT NULL DEFAULT 0,
  `scanned_quantity` int(11) NOT NULL DEFAULT 0,
  `status` varchar(40) NOT NULL DEFAULT 'PLANNED',
  `receipt_confirmed` tinyint(1) NOT NULL DEFAULT 0,
  `note` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `action_type_id` bigint(20) DEFAULT NULL,
  `material_type_id` bigint(20) DEFAULT NULL,
  `source_warehouse_id` bigint(20) DEFAULT NULL,
  `target_branch_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_marketing_history` (
  `id` bigint(20) NOT NULL,
  `campaign_id` bigint(20) NOT NULL,
  `event_type` varchar(80) NOT NULL,
  `details` text DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `quantity_delta` int(11) DEFAULT NULL,
  `material_code` varchar(120) DEFAULT NULL,
  `reverted` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_marketing_material_type` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 100
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_settings` (
  `setting_key` varchar(120) NOT NULL,
  `setting_value` text DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_tuya_sources` (
  `id` bigint(20) NOT NULL,
  `name` varchar(120) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `connection_mode` varchar(30) NOT NULL DEFAULT 'DIRECT_CLOUD',
  `homeassistant_source_id` bigint(20) DEFAULT NULL,
  `app_type` varchar(30) NOT NULL DEFAULT 'SMART_LIFE',
  `region` varchar(30) NOT NULL DEFAULT 'EUROPE',
  `client_id` varchar(255) NOT NULL,
  `client_secret` text DEFAULT NULL,
  `user_uid` varchar(255) DEFAULT NULL,
  `account_username` varchar(255) DEFAULT NULL,
  `account_password` text DEFAULT NULL,
  `country_code` varchar(12) DEFAULT NULL,
  `app_schema` varchar(80) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_warehouse_location` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `code` varchar(80) DEFAULT NULL,
  `warehouse_type_id` bigint(20) DEFAULT NULL,
  `branch_id` bigint(20) DEFAULT NULL,
  `address` varchar(500) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `note` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_warehouse_type` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 100
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `invoice_workflow_history` (
  `id` bigint(20) NOT NULL,
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) DEFAULT NULL,
  `from_status` varchar(32) DEFAULT NULL,
  `to_status` varchar(32) NOT NULL,
  `note` text DEFAULT NULL,
  `changed_by` varchar(255) DEFAULT NULL,
  `changed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `invoice_workflow_state` (
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ENTWURF',
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `payment_workflow_document` (
  `id` bigint(20) NOT NULL,
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) DEFAULT NULL,
  `document_type` varchar(32) NOT NULL,
  `title` varchar(255) NOT NULL,
  `language` varchar(8) NOT NULL DEFAULT 'de',
  `pdf_data` longblob NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `created_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `payment_workflow_history` (
  `id` bigint(20) NOT NULL,
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) DEFAULT NULL,
  `from_status` varchar(32) DEFAULT NULL,
  `to_status` varchar(32) NOT NULL,
  `amount` decimal(15,2) DEFAULT NULL,
  `note` text DEFAULT NULL,
  `changed_by` varchar(255) DEFAULT NULL,
  `changed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `payment_workflow_state` (
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'OFFEN',
  `amount_due` decimal(15,2) NOT NULL DEFAULT 0.00,
  `paid_amount` decimal(15,2) NOT NULL DEFAULT 0.00,
  `due_date` date DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `rechnungstext_gesellschaft_zuordnung` (
  `ID` int(11) NOT NULL,
  `RGESELLSCHAFTS_ID` int(11) NOT NULL,
  `ANREDE_ID` int(11) DEFAULT NULL,
  `RECHNUNGSTEXT_ID` int(11) DEFAULT NULL,
  `RECHTLICHER_HINWEIS_ID` int(11) DEFAULT NULL,
  `GRUSSFORMEL_ID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `task_workflow_settings` (
  `setting_key` varchar(120) NOT NULL,
  `setting_value` varchar(255) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_czech` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_dutch` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_italian` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_polish` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_portuguese` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_russian` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_spanish` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_swedish` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_turkish` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

ALTER TABLE `account_passkeys`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_account_passkeys_credential_id` (`credential_id`),
  ADD KEY `idx_account_passkeys_account_id` (`account_id`),
  ADD KEY `idx_account_passkeys_active` (`active`);

ALTER TABLE `communication_workflow_item`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_comm_status` (`status`),
  ADD KEY `idx_comm_due` (`due_date`),
  ADD KEY `idx_comm_responsible` (`responsible`);

ALTER TABLE `communication_workflow_settings`
  ADD PRIMARY KEY (`setting_key`);

ALTER TABLE `gam_discovery_builtin_sources`
  ADD PRIMARY KEY (`source_key`);

ALTER TABLE `gam_discovery_device_type_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_gam_discovery_type_history_identity` (`identity_key`);

ALTER TABLE `gam_discovery_merge_settings`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_discovery_registered_devices`
  ADD PRIMARY KEY (`identity_key`);

ALTER TABLE `gam_fritzbox_sources`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_homeassistant_sources`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_marketing_action_type`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_marketing_action_type_name` (`name`);

ALTER TABLE `gam_marketing_campaign`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_marketing_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_marketing_history_campaign` (`campaign_id`);

ALTER TABLE `gam_marketing_material_type`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_marketing_material_type_name` (`name`);

ALTER TABLE `gam_settings`
  ADD PRIMARY KEY (`setting_key`);

ALTER TABLE `gam_tuya_sources`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_warehouse_location`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_warehouse_location_name` (`name`);

ALTER TABLE `gam_warehouse_type`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_warehouse_type_name` (`name`);

ALTER TABLE `invoice_workflow_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_invoice_workflow_history_invoice` (`invoice_number`,`company_id`,`changed_at`);

ALTER TABLE `invoice_workflow_state`
  ADD PRIMARY KEY (`invoice_number`,`company_id`);

ALTER TABLE `payment_workflow_document`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_payment_document_invoice` (`invoice_number`,`company_id`,`created_at`);

ALTER TABLE `payment_workflow_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_payment_workflow_history` (`invoice_number`,`company_id`,`changed_at`);

ALTER TABLE `payment_workflow_state`
  ADD PRIMARY KEY (`invoice_number`,`company_id`);

ALTER TABLE `rechnungstext_gesellschaft_zuordnung`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `ux_rechnungstext_gesellschaft` (`RGESELLSCHAFTS_ID`);

ALTER TABLE `task_workflow_settings`
  ADD PRIMARY KEY (`setting_key`);

ALTER TABLE `translation_czech`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_dutch`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_italian`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_polish`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_portuguese`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_russian`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_spanish`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_swedish`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_turkish`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `account_passkeys`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `communication_workflow_item`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `gam_discovery_device_type_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

ALTER TABLE `gam_fritzbox_sources`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `gam_homeassistant_sources`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `gam_marketing_action_type`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=269;

ALTER TABLE `gam_marketing_campaign`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `gam_marketing_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `gam_marketing_material_type`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=403;

ALTER TABLE `gam_tuya_sources`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

ALTER TABLE `gam_warehouse_location`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=135;

ALTER TABLE `gam_warehouse_type`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=135;

ALTER TABLE `invoice_workflow_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

ALTER TABLE `payment_workflow_document`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

ALTER TABLE `payment_workflow_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=65;

ALTER TABLE `rechnungstext_gesellschaft_zuordnung`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `translation_czech`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_dutch`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_italian`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_polish`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_portuguese`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_russian`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_spanish`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_swedish`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_turkish`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

-- Sichere technische Grundwerte ohne Praxis- oder Zugangsdaten.
INSERT IGNORE INTO `gam_discovery_merge_settings` (`id`,`updated_by`) VALUES (1,'system');

SET FOREIGN_KEY_CHECKS=1;
