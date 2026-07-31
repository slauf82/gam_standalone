# Schritt 40k34a – Android-Geräteidentität und Discovery

## Ziel

Beginn des Android-Blocks. Android-Geräte werden vollständig über die
bestehende Geräteidentität verwaltet - keine Sonderlösung, keine zweite
Geräteverwaltung, keine neue Discovery. In diesem Schritt werden
ausschließlich bereits vorhandene Discovery-Informationen (mDNS, SSDP, Home
Assistant, SNMP) intelligenter ausgewertet, um Android-Geräte auch ganz ohne
ADB möglichst zuverlässig zu erkennen. ADB folgt erst in 40k34b.

## Wichtiger Fund: mDNS-Diensttyp wurde bisher verworfen

Wie schon bei der SNMP-sysDescr (40k33b9) zeigte die Bestandsaufnahme dasselbe
Muster bei mDNS und SSDP: `NativeMdnsDiscovery` erkennt bereits gezielt den
mDNS-Diensttyp `_googlecast._tcp` (Chromecast/Android TV) sowie ein TXT-
Record - beides wurde bisher ausschließlich für den internen Identitäts-
schlüssel verwendet und für die Kategorie komplett verworfen: **jedes**
mDNS-Gerät wurde unabhängig vom erkannten Dienst als generisches
"Netzwerkgerät" registriert. Bei SSDP wurde nur der SERVER-Header für den
Namen genutzt, der volle Antworttext (der bei Chromecast/Android-TV-Geräten
oft "dial-multiscreen-org" oder ähnliche Hinweise enthält) ebenfalls
verworfen. Beide Lücken wurden in diesem Schritt geschlossen.

## Geänderte/neue Dateien

Backend:
- `backend/.../inventory/AndroidDeviceClassifier.java` (neu) – reine,
  zustandslose Textklassifizierung ohne Netzwerkzugriff. Wertet bereits
  vorhandene Signale aus (mDNS-Diensttyp/TXT, SSDP-Antworttext, Name,
  bekannter Hersteller) und liefert nur bei einem tatsächlich vorhandenen
  Hinweis eine Einstufung - keine Vermutungen.
- `backend/.../inventory/NativeMdnsDiscovery.java` – zusätzlicher, bereits
  etablierter mDNS-Diensttyp `_androidtvremote2._tcp.local` (Android TV/
  Google TV Fernbedienungs-Pairing) ergänzt.
- `backend/.../inventory/DeviceDiscoveryService.java` – `discoverMdns()` und
  `discoverSsdp()` nutzen jetzt den Android-Klassifizierer, statt den
  bereits vorhandenen Diensttyp/Antworttext zu verwerfen.
- `backend/.../inventory/HomeAssistantDeviceSource.java` – dieselbe
  Klassifizierung zusätzlich für über Home Assistant eingebundene
  `media_player`-Entitäten (Chromecast/Android TV), damit diese nicht als
  generisches "Audio & Receiver" enden.
- `backend/.../inventory/SnmpDiscoveryService.java` – dieselbe
  Klassifizierung auch für den seltenen Fall, dass ein Android-Gerät SNMP
  anbietet ("falls vorhanden", wie im Auftrag beschrieben).
- `backend/.../inventory/DeviceIdentityService.java` – `platformOf()` um
  "Android" erweitert (dieselbe, bereits für Linux/Windows/macOS bestehende
  Methode, nur eine weitere Zeile).
- `backend/.../inventory/DiscoveryRegistrationRepository.java` –
  `categorySpecificity()` um den generischen Android-Fallback ("Android-
  Gerät") als Plattform-Stufe (Rang 1, analog zu "Server / Linux-System")
  ergänzt, damit eine spätere spezifischere Android-Kategorie (z.B. ab
  40k34b) diesen generischen Fallback weiterhin korrekt überschreiben kann.

Frontend:
- `frontend/src/main.tsx` – die bereits bestehenden (aber unabhängig vom
  Discovery-Backend im Frontend dupliziert vorhandenen) Kategorisierungs-
  Regex für die Berichts-/Inventaransicht um "shield"/"google tv" (Fernseher
  & Multimedia) sowie "kindle"/"e-book"/"ereader" (Smartphones & Tablets)
  ergänzt - keine neue Reportkategorie, nur zusätzliche Schlüsselwörter in
  der bereits vorhandenen Taxonomie.

Dokumentation:
- `docs/40k34a-android-discovery.md` (diese Datei).

## Architekturentscheidungen

- **Keine neue Geräteverwaltung, keine zweite Geräteidentität:** Android-
  Geräte durchlaufen exakt dieselbe `DiscoveredDevice`/`recordDiscoveryHit`/
  `gam_discovery_registered_devices`-Pipeline wie jede andere Plattform. Es
  wurde keine einzige neue Tabelle, kein neuer Endpunkt und keine neue
  Merge-/Integritätslogik für Android eingeführt.
- **Keine zweite Discovery:** Es wurde kein neuer Discovery-Dienst gebaut.
  Stattdessen wurden ausschließlich bereits vorhandene, aber bisher
  ungenutzte Informationen aus bestehenden Quellen (mDNS-Diensttyp/TXT,
  SSDP-Antworttext, Home-Assistant-Attribute, SNMP-sysDescr) besser
  ausgewertet - derselbe Ansatz wie beim SNMP-Fix in 40k33b9.
- **Ein einziger, wiederverwendeter Klassifizierer:** `AndroidDeviceClassifier`
  wird von vier verschiedenen, bereits vorhandenen Quellen aufgerufen (mDNS,
  SSDP, Home Assistant, SNMP) - keine vier separate Android-Erkennungen.
- **Keine unsicheren Vermutungen:** Es wird ausschließlich klassifiziert,
  wenn ein konkreter Text-Hinweis (Diensttyp, Name, TXT-Eintrag) tatsächlich
  vorliegt. Ohne Hinweis bleibt die bisherige, unveränderte Kategorisierung
  bestehen (`Optional.empty()`).
- **Plattform-/Geräteklassenanzeige:** nutzt ausschließlich bereits
  bestehende, generische Felder (`Plattform`, `Kategorie`) - keine neue
  Android-spezifische Oberfläche. Da die Gerätekategorie jetzt direkt das
  passende Symbol enthält (z.B. "📱 Smartphone", "📺 Android TV"), erscheint
  es automatisch überall dort, wo die Kategorie ohnehin schon als Text
  angezeigt wird - ohne jede Frontend-Änderung an der Darstellungslogik.

## Erkannte Geräteklassen

| Kategorie | Auslösendes Signal (bereits vorhanden) |
|---|---|
| 📺 Fire TV | Name/Diensttext enthält "amzn-wplay"/"fire tv" |
| 🎮 Nvidia Shield | Namenshinweis "shield" |
| 📺 Google TV | mDNS-Diensttyp `_androidtvremote(2)._tcp` |
| 📺 Android TV | mDNS-Diensttyp `_googlecast._tcp` / Chromecast-Hinweis |
| 📖 E-Book Reader | Namenshinweis Kindle/E-Book/E-Reader |
| 🖥 Android Display | Namenshinweis Kiosk/Signage/Panel |
| 📱 Tablet | Namenshinweis Tablet/Galaxy Tab/MediaPad/MatePad |
| 📱 Smartphone | Namenshinweis Smartphone-Modell (Galaxy S/A/Z, Pixel, Redmi, Poco, …) |
| Android-Gerät | Allgemeiner "android"-Hinweis ohne spezifischere Zuordnung |

## Hersteller

Bereits von anderen Quellen ermittelte Hersteller werden unverändert
übernommen. Nur wenn kein Hersteller bekannt ist, wird er über denselben,
bereits im Projekt etablierten Textmuster-Abgleich abgeleitet (analog zu
`SnmpDiscoveryService.manufacturer()`): Samsung, Google, Sony, Xiaomi,
Lenovo, Huawei, Amazon, Nvidia, OnePlus, Motorola, Honor, Realme, Nothing.

## Rollen

Erkannte Rollen (Streaming, Fernseher, Digital Signage, Bedienpanel, Tablet,
Telefon, Multimedia) werden über dasselbe "Erkannte Rollen"-Detailfeld
gespeichert, das bereits für Linux verwendet wird (40k33b6a) - keine
Android-spezifische Rollenverwaltung.

## Reports

Da die Gerätekategorie jetzt korrekt gesetzt wird, greifen die bereits
bestehenden Report-/Inventar-Kategorisierungsregeln im Frontend automatisch:
Android-TV-artige Geräte landen in "Fernseher & Multimedia", Smartphones/
Tablets in "Smartphones & Tablets" - beides bereits existierende Kategorien.
Es wurde keine neue Reportkategorie angelegt.

## Logging

Für jedes über mDNS/SSDP erkannte Android-Gerät wird protokolliert: Name,
vergebene Geräteklasse und die auslösenden Gründe (`log.info`, keine
übermäßigen Debug-Ausgaben, wie im Auftrag gefordert).

## Tests / durchgeführte Prüfungen in dieser Umgebung

Kein JDK, kein funktionierendes `npm install` in dieser Sandbox - kein echter
Build möglich. Stattdessen geprüft:

- Klammernbilanz aller sieben geänderten/neuen Backend-Dateien (ausgeglichen).
- Kreuzabgleich aller `AndroidDeviceClassifier.classify(...)`-Aufrufe gegen
  die tatsächliche Methodensignatur.
- Isolierter TypeScript-Transpile-Lauf über die vollständige `main.tsx` nach
  den Regex-Erweiterungen - 0 Diagnosen.
- Manuelle Prüfung der Regex-Reihenfolge im Frontend (Fernseher-Erkennung
  läuft bewusst VOR der Smartphone-Erkennung, damit z.B. "Android TV" nicht
  fälschlich als Smartphone eingeordnet wird).

**Nicht durchgeführt:** echter Maven-/Vite-Build, automatisierte Tests, Test
gegen ein echtes Android-Gerät/Chromecast/Fire-TV-Gerät im Netzwerk. Bitte
vor dem Produktiveinsatz verifizieren.

## Bekannte Einschränkungen

- HTTP/HTTPS-basierte aktive Android-Erkennung (z.B. Server-Header oder
  Titel-Abruf) wurde nicht implementiert - der einzige bestehende
  HTTP-Titel-Mechanismus ist an die Linux-Nachprüfung gebunden (40k33b9) und
  lässt sich nicht ohne Weiteres auf beliebige, nicht-Linux-getriggerte
  Geräte übertragen, ohne eine neue, generische Discovery-Erweiterung zu
  bauen. Das wäre über den Rahmen "keine zweite Discovery" hinausgegangen.
- 🖥 Android Display (Kiosk/Signage) und 📖 E-Book Reader haben keine exakt
  passende bestehende Report-Kategorie; sie wurden den am ehesten passenden
  bestehenden Kategorien zugeordnet (Smartphones & Tablets bzw. keine
  Zuordnung/"Sonstige Geräte"), statt eine neue Kategorie zu erfinden.
- Ohne ADB bleibt die Erkennung auf das beschränkt, was Netzwerk-Discovery
  hergibt - viele Android-Smartphones/-Tablets im normalen WLAN-Betrieb
  senden keine eindeutigen mDNS-/SSDP-Signaturen und werden daher weiterhin
  nur generisch (oder gar nicht als Android) erkannt. Das ist erwartet und
  wird erst mit ADB in 40k34b umfassender.

## Manuelle Testanleitung

1. Einen Chromecast/ein Android-TV-Gerät im Netzwerk suchen lassen: Kategorie
   muss "📺 Android TV" (oder bei unterstützter Fernbedienung "📺 Google TV")
   zeigen, Plattform "Android", Hersteller falls erkennbar gesetzt.
2. Ein Fire-TV-Gerät suchen lassen: Kategorie "📺 Fire TV", Hersteller
   "Amazon".
3. Ein über Home Assistant eingebundenes Android-TV-`media_player`-Gerät
   prüfen: muss ebenfalls die passende Android-Kategorie erhalten, nicht
   "Audio & Receiver".
4. Ein erkanntes Android-TV-Gerät in den Gerätebestand/Report übernehmen:
   muss automatisch unter "Fernseher & Multimedia" erscheinen.
5. Regressionstest: bestehende mDNS-/SSDP-Erkennung nicht-Android-typischer
   Geräte (Drucker, Home Assistant, Cockpit usw.) muss unverändert
   funktionieren; bestehende Merge-/Integritätsprüfung/Linux-Funktionen
   bleiben unverändert.
