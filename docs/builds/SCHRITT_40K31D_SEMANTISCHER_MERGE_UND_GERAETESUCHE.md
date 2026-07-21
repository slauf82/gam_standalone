# Schritt 40k31d – Semantischer Merge und Gerätesuche

## Semantische Geräteidentität

Discovery normalisiert Gerätenamen quellenübergreifend. Dabei werden insbesondere:

- Groß- und Kleinschreibung,
- Leerzeichen und Sonderzeichen,
- deutsche Umlaute (`ä` ↔ `ae`, `ö` ↔ `oe`, `ü` ↔ `ue`, `ß` ↔ `ss`),
- Home-Assistant-Entity-Präfixe wie `climate.`

vereinheitlicht.

Damit können beispielsweise `Luftwärmetauscher`, `luftwaermetauscher` und
`climate.luftwaermetauscher` als dieselbe physische Geräteidentität erkannt werden.
Die automatische semantische Zusammenführung gilt nur für hinreichend spezifische Namen
und unabhängige Discovery-Quellen. Harte Identitätskonflikte bleiben weiterhin geschützt.

## Persistente Registrierungen

Bereits gespeicherte semantische Dubletten werden beim Start einer neuen Discovery-Runde
konsolidiert. Quellen, Trefferzahlen und die jeweils besten bekannten Identitätsdaten werden
dabei zusammengeführt.

## Gerätesuche

Die Liste der registrierten Discovery-Geräte besitzt nun eine sofort reagierende Suche über:

- Gerätename und Typ,
- IP-Adresse,
- MAC-Adresse und Seriennummer,
- Hersteller,
- Quelle und Status.

Trefferzahl und Zurücksetzen-Funktion werden direkt angezeigt.
