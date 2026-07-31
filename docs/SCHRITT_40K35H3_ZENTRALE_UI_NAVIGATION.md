# Schritt 40k35h3 – zentrale UI-Navigation

Grundsatzentscheidung: Öffnet eine Benutzeraktion einen neuen Dialog, Status- oder Diagnosebereich außerhalb des sichtbaren Ausschnitts, führt GAM den Benutzer automatisch dorthin.

Umgesetzt über den zentralen React-Hook `useAttentionNavigation` mit einheitlichem Smooth-Scroll, Sticky-Header-Abstand und optionalem Fokus auf das erste Eingabefeld.

Bereits angebunden:
- Android-Gerät koppeln
- Android-Gerät verbinden
- Android-Verbindung prüfen / Inventarisierung / Ergebnisstatus
- Gerätesuche-Fortschritt
- automatisch oder manuell geöffnete Discovery-Diagnose
- WinRM-Konfiguration

SSH erhält denselben Mechanismus, sobald der eigene Konfigurationsdialog in einem folgenden Schritt vorhanden ist.
