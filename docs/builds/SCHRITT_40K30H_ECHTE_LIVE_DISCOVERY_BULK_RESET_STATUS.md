# Schritt 40k30h – echte Live-Discovery, automatische Persistenz und vollständige Rücksetzwege

- Die Discovery startet als asynchrone Session und liefert Treffer, Fortschritt und Diagnosen über einen getrennten SSE-Request.
- Der globale Abschluss wird erst nach den bereits live ausgelieferten Einzeltreffern gesendet.
- AUTO_REGISTER speichert jeden neuen Treffer unmittelbar in `gam_discovery_registered_devices`.
- Wiedererkannte Identitäten werden aktualisiert statt dupliziert; aus 52 bekannten und 18 neuen Treffern werden 70 registrierte Geräte.
- F5 leert nur die temporären Discovery-Ergebnisse, nicht die dauerhaft registrierten Geräte.
- Sammelaktion „Registrierung für alle aufheben“ ist mit Rückfrage verfügbar.
- Sammelaktion „Gesamten Gerätebestand leeren“ stuft alle Geräte transaktional nach „Registriert“ zurück und verlangt zwei Bestätigungen.
- Bei einem Fehler wird die Bestandsleerung vollständig zurückgerollt.
