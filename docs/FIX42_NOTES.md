# GAM 2.1.0 – fix42: Plattformklassifizierung / Merge

- ADB- und Apple-Transportnachweise erhalten Vorrang vor historisch falsch gespeicherten Geraetekategorien.
- Die generische Klasse „Smartphones & Tablets“ wird nicht mehr pauschal als Android inventarisiert.
- Bei reinem ARP/FRITZ-Netzwerkfund wird die alte iPadOS-Kategorie nicht als gesicherte Apple-Plattform akzeptiert.
- Klar erkennbare Android-Modellnamen (`android-*`, Samsung/Galaxy, Doogee, Fossibot) werden als Plattformhinweis genutzt, sofern kein Apple-Transportnachweis vorliegt. Das ist eine Heuristik, kein Hardwarebeweis.
- Merge-Engine blockiert Android-/Apple-Mobile-Konflikte vor einer Bewertung der Identitaetsmerkmale, auch bei alten falschen Typzuordnungen.
- Apple-Discovery, RemotePairing, PMD3, USERSPACE_RSD und die Inventarisierungstransporte selbst sind unberuehrt.
- Wichtig: Eine automatische vollstaendige Plattformrekonstruktion aller historischen Netzwerkgeraete ist ohne belegende Protokolle nicht moeglich.
