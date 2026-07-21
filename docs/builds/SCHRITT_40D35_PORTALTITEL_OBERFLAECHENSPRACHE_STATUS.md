# Schritt 40d35 – Portaltitel in Oberflächensprache

## Ziel
Die sichtbaren Belegtitel im Patientenportal gehören zur Benutzeroberfläche und müssen daher in der gewählten Oberflächensprache erscheinen. Der Inhalt der PDF und das Vorlesen der Rechnung beziehungsweise des Zahlungsdokuments bleiben weiterhin an die ausgewählte PDF-Sprache gebunden.

## Umsetzung
- Rechnungstitel in der Portaltabelle verwenden `uiLang`.
- Stornorechnungsbezeichnung und Status „bereits storniert“ verwenden `uiLang`.
- Zahlungserinnerung, 1.–3. Mahnung und Inkasso-Titel in der Portaltabelle verwenden `uiLang`.
- Der sprachabhängige Zusatz „zu / for invoice / pour la facture …“ verwendet ebenfalls `uiLang`.
- PDF-Links und PDF-Vorlese-Endpunkte erhalten weiterhin `invoiceLang`.
- PDF-Inhalt, PDF-Dokumenttitel und Rechnungsvorlesen bleiben damit in der PDF-Sprache.
- Portalinformationen und sichtbare Tabellenbezeichnungen bleiben in der Oberflächensprache.

## Sprachtrennung
- Patientenportal-Oberfläche und sichtbare Dokumenttitel: Oberflächensprache
- PDF, PDF-interner Titel und Vorlesen des Dokuments: PDF-Sprache
