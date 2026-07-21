# Schritt 40k25a – Geräteverzeichnis, Listenmodus und dauerhafte Registrierung

- Laufzeitfehler `enrichDevice is not defined` nach Abschluss der Discovery behoben.
- Manueller Modus: neue Geräte stehen oben, nach fachlichen Kategorien sortiert; registrierte Alt- und Neugeräte stehen unten gemeinsam.
- Automatischer Modus: neue Discovery-Treffer werden direkt dauerhaft im GAM-Geräteverzeichnis gespeichert; die obere Liste entfällt.
- Der Registrierungsmodus wird in `gam_discovery_builtin_sources` gespeichert und bleibt bei Build-Updates erhalten.
- Registrierte Geräte werden über die bestehende Inventar-API in der GAM-Datenbank gespeichert.
- Frontend-Build erfolgreich.
