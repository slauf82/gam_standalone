# 40k36t11

Android-Autoinventarisierung verwendet jetzt dieselbe konservative Evidenzlogik
wie Windows, Linux und macOS.

Eine automatische Android-Vollinventarisierung startet nur bei:
- starker Android-/ADB-Evidenz, z.B. verifizierter ADB-Inventarisierung,
  `getprop`-/Build-Informationen oder eindeutigem Android-Gerätetyp; oder
- konkret für die Geräteidentität hinterlegtem ADB-Ziel (`adbHost`).

Ein schwacher Android-Hinweis bzw. bloß `platformOf()='Android'` reicht nicht mehr.

Das Debug-Log weist getrennt aus:
- starke Android-Evidenz
- ADB konfiguriert
- schwacher Android-Hinweis allein

Für iOS/iPadOS soll dieselbe Evidenzregel bereits beim Aufbau von 40k37
von Anfang an gelten.
