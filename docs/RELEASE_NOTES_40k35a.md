# Release Notes 40k35a

Android-Inventarisierung stabilisiert und beschleunigt. Große ADB-Ausgaben werden jetzt vollständig aus dem Prozesspuffer gelesen, ohne unbegrenzt gespeichert zu werden. Dadurch entfallen künstliche Deadlocks und unnötige Timeout-Wartezeiten. App-Inventare unterscheiden nun zwischen vollständig, erfolgreich mit Hinweisen und tatsächlich teilweise. Zusätzlich wurde die UTF-8-Ausgabe für Backend-Konsole und Logdatei vereinheitlicht.
