package de.kopfzentrum.gam.invoice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class TranslationService {
  private final JdbcTemplate jdbc;

  public TranslationService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public String invoice(String key, String language) {
    return resolve(key, language, fallback(key, normalize(language)), true);
  }

  public String resolve(String key, String language, String fallback) {
    return resolve(key, language, fallback, true);
  }

  public String resolve(String key, String language, String fallback, boolean persistMissing) {
    String lang = normalize(language);
    String description = prefix(lang) + "." + key;
    String table = table(lang);

    String translated = find(table, description);
    if (!blank(translated)) return translated;

    String value = fallback == null || fallback.isBlank() ? fallback(key, lang) : fallback;
    if (blank(value)) value = key;

    // Schritt 28d: Fehlende neue Rechnungstexte werden wie im alten GAM automatisch in die Translation-Tabelle eingetragen.
    // LibreTranslate kann später an dieser Stelle vorgeschaltet werden. Aktuell wird ein stabiler Offline-Fallback gespeichert.
    if (persistMissing) ensure(table, description, value);

    if (!"de".equals(lang)) {
      String german = find("translation_german", "GERMAN." + key);
      if (!blank(german)) return value;
    }

    return value;
  }

  private String find(String table, String description) {
    try {
      return jdbc.query("SELECT TRANSLATED_TEXT FROM " + table + " WHERE TRANSLATE_DESCRIPTION = ? ORDER BY ID DESC LIMIT 1", rs -> {
        if (rs.next()) return rs.getString(1);
        return null;
      }, description);
    } catch (Exception ignored) {
      return null;
    }
  }

  private void ensure(String table, String description, String text) {
    if (blank(description) || blank(text)) return;
    try {
      Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE TRANSLATE_DESCRIPTION = ?", Integer.class, description);
      if (count != null && count > 0) return;
      jdbc.update("INSERT INTO " + table + " (TRANSLATED_TEXT, TRANSLATE_DESCRIPTION) VALUES (?, ?)", text, description);
    } catch (Exception ignored) {
      // Falls die Demo-Datenbank die Tabellen noch nicht enthält, darf die Rechnungserzeugung nicht scheitern.
    }
  }

  private static String normalize(String language) {
    if (language == null || language.isBlank()) return "de";
    String l = language.trim().toLowerCase(Locale.ROOT);
    if (l.startsWith("en")) return "en";
    if (l.startsWith("fr")) return "fr";
    if (l.startsWith("uk") || l.startsWith("ua")) return "uk";
    if (l.startsWith("it")) return "it";
    if (l.startsWith("sv") || l.startsWith("se")) return "sv";
    if (l.startsWith("tr")) return "tr";
    if (l.startsWith("ru")) return "ru";
    return "de";
  }

  private static String table(String lang) {
    return switch (lang) {
      case "en" -> "translation_english";
      case "fr" -> "translation_french";
      case "uk" -> "translation_ukrainian";
      case "it" -> "translation_italian";
      case "sv" -> "translation_swedish";
      case "tr" -> "translation_turkish";
      case "ru" -> "translation_russian";
      default -> "translation_german";
    };
  }

  private static String prefix(String lang) {
    return switch (lang) {
      case "en" -> "ENGLISH";
      case "fr" -> "FRENCH";
      case "uk" -> "UKRAINIAN";
      case "it" -> "ITALIAN";
      case "sv" -> "SWEDISH";
      case "tr" -> "TURKISH";
      case "ru" -> "RUSSIAN";
      default -> "GERMAN";
    };
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String fallback(String key, String lang) {
    return switch (lang) {
      case "en" -> EN.getOrDefault(key, DE.getOrDefault(key, key));
      case "fr" -> FR.getOrDefault(key, DE.getOrDefault(key, key));
      case "uk" -> UK.getOrDefault(key, DE.getOrDefault(key, key));
      case "it" -> IT.getOrDefault(key, DE.getOrDefault(key, key));
      case "sv" -> SV.getOrDefault(key, DE.getOrDefault(key, key));
      case "tr" -> TR.getOrDefault(key, DE.getOrDefault(key, key));
      case "ru" -> RU.getOrDefault(key, DE.getOrDefault(key, key));
      default -> DE.getOrDefault(key, key);
    };
  }

  private static final Map<String, String> DE = Map.ofEntries(
    Map.entry("invoice", "Rechnung"),
    Map.entry("proformaInvoice", "Proforma-Rechnung"),
    Map.entry("credit", "Gutschrift"),
    Map.entry("cancellation", "Stornorechnung"),
    Map.entry("paymentAdvice", "Zahlungsavis"),
    Map.entry("invoiceRecipient", "Rechnungsempfänger"),
    Map.entry("invoiceAmount", "Menge"),
    Map.entry("invoiceDescription", "Beschreibung"),
    Map.entry("invoiceTaxRate", "Steuersatz"),
    Map.entry("invoiceSinglePrice", "Einzelpreis"),
    Map.entry("invoiceTotalPrice", "Gesamtpreis"),
    Map.entry("invoiceProductCode", "Code"),
    Map.entry("invoiceDate", "Rechnungsdatum"),
    Map.entry("treatmentDate", "Behandlungsdatum"),
    Map.entry("dueDate", "Fälligkeitsdatum"),
    Map.entry("invoiceCustomerFile", "Kundendatei"),
    Map.entry("invoiceUser", "Benutzer"),
    Map.entry("invoicePaymentMethod", "Zahlungsart"),
    Map.entry("invoiceSalutationLabel0", "Sehr geehrte(r) <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>,"),
    Map.entry("invoiceInvoiceTextLabel0", "sehr gern haben wir <SieIhrKind>, <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname> am <Behandlungsdatum> behandelt und hoffen, dass Sie einen angenehmen Aufenthalt bei uns hatten. -br- Wir erlauben uns, Ihre Behandlung wie folgt in Rechnung zu stellen:"),
    Map.entry("invoiceLawHintLabel0", "Gern beantworten wir Ihnen weitere Fragen. -br- Diese Liquidation wurde maschinell erstellt und ist daher ohne Unterschrift oder Stempel gültig."),
    Map.entry("invoiceGreetingsLabel0", "Mit freundlichen Grüßen -br--br- Ihr(e) <Gesellschaftsname>"),
    Map.entry("invoiceYou", "Sie"),
    Map.entry("net", "Netto"),
    Map.entry("vat", "MwSt"),
    Map.entry("gross", "Gesamt"),
    Map.entry("invoiceReducement", "Rabatt"),
    Map.entry("invoiceReducedTotal", "Endbetrag nach Abzug"),
    Map.entry("invoiceCoupon", "Gutschein"),
    Map.entry("invoiceCouponText", "Gutscheintext"),
    Map.entry("invoiceCouponAmount", "Gutscheinbetrag"),
    Map.entry("invoiceInstallments", "Ratenzahlung"),
    Map.entry("invoiceInstallmentApprox", "Raten à ca."),
    Map.entry("invoiceNoRecipient", "Kein .lbd-Empfänger gefunden - bitte konfigurieren."),
    Map.entry("invoiceZugferdNote", "Diese Rechnung enthält die maschinenlesbare ZUGFeRD/Factur-X XML im PDF."),
    Map.entry("invoiceFallbackPdfNote", "Debug-/Fallback-PDF; verbindlich ist der ZUGFeRD/Factur-X-Export."),
    Map.entry("bank", "Bankverbindung"),
    Map.entry("invoiceTaxNumberVatId", "Steuernummer/USt-ID"),
    Map.entry("invoicePreviewTitle", "Rechnungsvorschau"),
    Map.entry("invoicePreviewHelp", "Rechter Vorschau-Teil nach Alt-GAM-Prinzip: Anrede, Rechnungstext, Positionen, rechtlicher Hinweis und Grußformel."),
    Map.entry("invoiceNoLines", "Noch keine Positionen übernommen."),
    Map.entry("invoicePortalTitle", "Digitales Rechnungsportal"),
    Map.entry("invoicePortalHelp", "Wählen Sie die gewünschte Sprache und laden Sie Ihre Rechnung erneut herunter."),
    Map.entry("invoicePortalQrHint", "Diese Rechnung digital abrufen: QR-Code scannen oder Link öffnen."),
    Map.entry("invoiceReadAloud", "Vorlesen")
  );

  private static final Map<String, String> EN = Map.ofEntries(
    Map.entry("invoice", "Invoice"),
    Map.entry("proformaInvoice", "Proforma invoice"),
    Map.entry("credit", "Credit note"),
    Map.entry("cancellation", "Cancellation invoice"),
    Map.entry("paymentAdvice", "Payment advice"),
    Map.entry("invoiceRecipient", "Invoice recipient"),
    Map.entry("invoiceAmount", "Quantity"),
    Map.entry("invoiceDescription", "Description"),
    Map.entry("invoiceTaxRate", "Tax rate"),
    Map.entry("invoiceSinglePrice", "Unit price"),
    Map.entry("invoiceTotalPrice", "Total price"),
    Map.entry("invoiceProductCode", "Code"),
    Map.entry("invoiceDate", "Invoice date"),
    Map.entry("treatmentDate", "Treatment date"),
    Map.entry("dueDate", "Due date"),
    Map.entry("invoiceCustomerFile", "Customer file"),
    Map.entry("invoiceUser", "User"),
    Map.entry("invoicePaymentMethod", "Payment method"),
    Map.entry("invoiceSalutationLabel0", "Dear <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>,"),
    Map.entry("invoiceInvoiceTextLabel0", "we were pleased to treat <SieIhrKind>, <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname> on <Behandlungsdatum> and hope that your stay with us was pleasant. -br- We hereby invoice the treatment as follows:"),
    Map.entry("invoiceLawHintLabel0", "We will be happy to answer any further questions. -br- This invoice was created electronically and is valid without signature or stamp."),
    Map.entry("invoiceGreetingsLabel0", "Kind regards -br--br- Your <Gesellschaftsname>"),
    Map.entry("invoiceYou", "you"),
    Map.entry("net", "Net"), Map.entry("vat", "VAT"), Map.entry("gross", "Total"),
    Map.entry("invoiceReducement", "Discount"),
    Map.entry("invoiceReducedTotal", "Total after deduction"),
    Map.entry("invoiceCoupon", "Voucher"),
    Map.entry("invoiceCouponText", "Voucher text"),
    Map.entry("invoiceCouponAmount", "Voucher amount"),
    Map.entry("invoiceInstallments", "Installment payment"),
    Map.entry("invoiceInstallmentApprox", "installments of approx."),
    Map.entry("invoiceNoRecipient", "No .lbd recipient file found - please configure it."),
    Map.entry("invoiceZugferdNote", "This invoice contains the machine-readable ZUGFeRD/Factur-X XML in the PDF."),
    Map.entry("invoiceFallbackPdfNote", "Debug/fallback PDF; the ZUGFeRD/Factur-X export is binding."),
    Map.entry("bank", "Bank details"),
    Map.entry("invoiceTaxNumberVatId", "Tax number/VAT ID"),
    Map.entry("invoicePreviewTitle", "Invoice preview"),
    Map.entry("invoicePreviewHelp", "Right-hand preview area based on the legacy GAM principle: salutation, invoice text, items, legal notice and closing."),
    Map.entry("invoiceNoLines", "No items have been added yet."),
    Map.entry("invoicePortalTitle", "Digital invoice portal"),
    Map.entry("invoicePortalHelp", "Select your preferred language and download your invoice again."),
    Map.entry("invoicePortalQrHint", "Open this invoice digitally: scan the QR code or open the link."),
    Map.entry("invoiceReadAloud", "Read aloud")
  );

  private static final Map<String, String> FR = Map.ofEntries(
    Map.entry("invoice", "Facture"),
    Map.entry("proformaInvoice", "Facture pro forma"),
    Map.entry("credit", "Avoir"),
    Map.entry("cancellation", "Facture d'annulation"),
    Map.entry("paymentAdvice", "Avis de paiement"),
    Map.entry("invoiceRecipient", "Destinataire de la facture"),
    Map.entry("invoiceAmount", "Quantité"),
    Map.entry("invoiceDescription", "Description"),
    Map.entry("invoiceTaxRate", "Taux de TVA"),
    Map.entry("invoiceSinglePrice", "Prix unitaire"),
    Map.entry("invoiceTotalPrice", "Prix total"),
    Map.entry("invoiceProductCode", "Code"),
    Map.entry("invoiceDate", "Date de facture"),
    Map.entry("treatmentDate", "Date de traitement"),
    Map.entry("dueDate", "Date d’échéance"),
    Map.entry("invoiceCustomerFile", "Dossier client"),
    Map.entry("invoiceUser", "Utilisateur"),
    Map.entry("invoicePaymentMethod", "Mode de paiement"),
    Map.entry("invoiceSalutationLabel0", "Madame, Monsieur <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>,"),
    Map.entry("invoiceInvoiceTextLabel0", "nous avons eu le plaisir de traiter <SieIhrKind>, <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname> le <Behandlungsdatum> et espérons que votre séjour chez nous a été agréable. -br- Nous vous facturons le traitement comme suit :"),
    Map.entry("invoiceLawHintLabel0", "Nous répondrons volontiers à toute question complémentaire. -br- Cette facture a été créée électroniquement et est valable sans signature ni cachet."),
    Map.entry("invoiceGreetingsLabel0", "Cordialement -br--br- Votre <Gesellschaftsname>"),
    Map.entry("invoiceYou", "vous"),
    Map.entry("net", "Net"), Map.entry("vat", "TVA"), Map.entry("gross", "Total"),
    Map.entry("invoiceReducement", "Remise"),
    Map.entry("invoiceReducedTotal", "Total après déduction"),
    Map.entry("invoiceCoupon", "Bon"),
    Map.entry("invoiceCouponText", "Texte du bon"),
    Map.entry("invoiceCouponAmount", "Montant du bon"),
    Map.entry("invoiceInstallments", "Paiement échelonné"),
    Map.entry("invoiceInstallmentApprox", "échéances d'environ"),
    Map.entry("invoiceNoRecipient", "Aucun fichier destinataire .lbd trouvé - veuillez le configurer."),
    Map.entry("invoiceZugferdNote", "Cette facture contient le XML ZUGFeRD/Factur-X lisible par machine dans le PDF."),
    Map.entry("invoiceFallbackPdfNote", "PDF de débogage/secours ; l'export ZUGFeRD/Factur-X fait foi."),
    Map.entry("bank", "Coordonnées bancaires"),
    Map.entry("invoiceTaxNumberVatId", "Numéro fiscal/TVA"),
    Map.entry("invoicePreviewTitle", "Aperçu de la facture"),
    Map.entry("invoicePreviewHelp", "Zone d'aperçu à droite selon le principe GAM historique : salutation, texte de facture, postes, avis juridique et formule de politesse."),
    Map.entry("invoiceNoLines", "Aucun poste n'a encore été ajouté."),
    Map.entry("invoicePortalTitle", "Portail numérique de factures"),
    Map.entry("invoicePortalHelp", "Choisissez la langue souhaitée et téléchargez à nouveau votre facture."),
    Map.entry("invoicePortalQrHint", "Ouvrir cette facture numériquement : scannez le QR code ou ouvrez le lien."),
    Map.entry("invoiceReadAloud", "Lire")
  );

  private static final Map<String, String> UK = Map.ofEntries(
    Map.entry("invoice", "Рахунок"),
    Map.entry("proformaInvoice", "Рахунок-проформа"),
    Map.entry("credit", "Кредит-нота"),
    Map.entry("cancellation", "Сторно-рахунок"),
    Map.entry("paymentAdvice", "Платіжне повідомлення"),
    Map.entry("invoiceRecipient", "Одержувач рахунку"),
    Map.entry("invoiceAmount", "Кількість"),
    Map.entry("invoiceDescription", "Опис"),
    Map.entry("invoiceTaxRate", "Ставка податку"),
    Map.entry("invoiceSinglePrice", "Ціна за одиницю"),
    Map.entry("invoiceTotalPrice", "Загальна ціна"),
    Map.entry("invoiceProductCode", "Код"),
    Map.entry("invoiceDate", "Дата рахунку"),
    Map.entry("treatmentDate", "Дата лікування"),
    Map.entry("dueDate", "Дата оплати"),
    Map.entry("invoiceCustomerFile", "Файл клієнта"),
    Map.entry("invoiceUser", "Користувач"),
    Map.entry("invoicePaymentMethod", "Спосіб оплати"),
    Map.entry("invoiceSalutationLabel0", "Шановний(а) <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>,"),
    Map.entry("invoiceInvoiceTextLabel0", "ми раді, що змогли надати послугу <SieIhrKind>, <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname> <Behandlungsdatum>, і сподіваємося, що ваше перебування у нас було приємним. -br- Виставляємо рахунок за лікування наступним чином:"),
    Map.entry("invoiceLawHintLabel0", "Ми із задоволенням відповімо на додаткові запитання. -br- Цей рахунок створено в електронному вигляді, тому він дійсний без підпису та печатки."),
    Map.entry("invoiceGreetingsLabel0", "З повагою -br--br- Ваш <Gesellschaftsname>"),
    Map.entry("invoiceYou", "вас"),
    Map.entry("net", "Нетто"), Map.entry("vat", "ПДВ"), Map.entry("gross", "Разом"),
    Map.entry("invoiceReducement", "Знижка"),
    Map.entry("invoiceReducedTotal", "Сума після вирахування"),
    Map.entry("invoiceCoupon", "Ваучер"),
    Map.entry("invoiceCouponText", "Текст ваучера"),
    Map.entry("invoiceCouponAmount", "Сума ваучера"),
    Map.entry("invoiceInstallments", "Оплата частинами"),
    Map.entry("invoiceInstallmentApprox", "платежі приблизно по"),
    Map.entry("invoiceNoRecipient", "Файл одержувача .lbd не знайдено - будь ласка, налаштуйте його."),
    Map.entry("invoiceZugferdNote", "Цей рахунок містить машинозчитуваний XML ZUGFeRD/Factur-X у PDF."),
    Map.entry("invoiceFallbackPdfNote", "Резервний PDF для перевірки; чинним є експорт ZUGFeRD/Factur-X."),
    Map.entry("bank", "Банківські реквізити"),
    Map.entry("invoiceTaxNumberVatId", "Податковий номер/ПДВ ID"),
    Map.entry("invoicePreviewTitle", "Попередній перегляд рахунку"),
    Map.entry("invoicePreviewHelp", "Права область попереднього перегляду за принципом старого GAM: звертання, текст рахунку, позиції, юридична примітка та заключна фраза."),
    Map.entry("invoiceNoLines", "Позиції ще не додано."),
    Map.entry("invoicePortalTitle", "Цифровий портал рахунків"),
    Map.entry("invoicePortalHelp", "Оберіть бажану мову та завантажте рахунок ще раз."),
    Map.entry("invoicePortalQrHint", "Відкрити цей рахунок цифрово: відскануйте QR-код або відкрийте посилання."),
    Map.entry("invoiceReadAloud", "Зачитати")
  );

  private static final Map<String, String> IT = Map.ofEntries(
    Map.entry("invoice", "Fattura"), Map.entry("proformaInvoice", "Fattura proforma"), Map.entry("credit", "Nota di credito"), Map.entry("cancellation", "Fattura di storno"), Map.entry("paymentAdvice", "Avviso di pagamento"),
    Map.entry("invoiceRecipient", "Destinatario della fattura"), Map.entry("invoiceAmount", "Quantità"), Map.entry("invoiceDescription", "Descrizione"), Map.entry("invoiceTaxRate", "Aliquota IVA"), Map.entry("invoiceSinglePrice", "Prezzo unitario"), Map.entry("invoiceTotalPrice", "Prezzo totale"), Map.entry("invoiceProductCode", "Codice"),
    Map.entry("invoiceDate", "Data fattura"), Map.entry("treatmentDate", "Data trattamento"), Map.entry("dueDate", "Scadenza"), Map.entry("invoiceCustomerFile", "File cliente"), Map.entry("invoiceUser", "Utente"), Map.entry("invoicePaymentMethod", "Metodo di pagamento"),
    Map.entry("invoiceSalutationLabel0", "Gentile <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>,"),
    Map.entry("invoiceInvoiceTextLabel0", "siamo stati lieti di trattare <SieIhrKind>, <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>, il <Behandlungsdatum> e speriamo che il soggiorno presso di noi sia stato piacevole. -br- Con la presente fatturiamo il trattamento come segue:"),
    Map.entry("invoiceLawHintLabel0", "Saremo lieti di rispondere a eventuali ulteriori domande. -br- Questa fattura è stata generata elettronicamente ed è valida senza firma o timbro."),
    Map.entry("invoiceGreetingsLabel0", "Cordiali saluti -br--br- Il vostro <Gesellschaftsname>"), Map.entry("invoiceYou", "Lei"),
    Map.entry("net", "Netto"), Map.entry("vat", "IVA"), Map.entry("gross", "Totale"), Map.entry("invoiceReducement", "Sconto"), Map.entry("invoiceReducedTotal", "Totale dopo la detrazione"), Map.entry("invoiceCoupon", "Buono"), Map.entry("invoiceInstallments", "Pagamento rateale"), Map.entry("invoiceInstallmentApprox", "rate di circa"),
    Map.entry("invoiceNoRecipient", "Nessun destinatario .lbd trovato - configurare."), Map.entry("invoiceZugferdNote", "Questa fattura contiene il file XML ZUGFeRD/Factur-X leggibile dalla macchina nel PDF."), Map.entry("invoiceFallbackPdfNote", "PDF di debug/fallback; vincolante è l'esportazione ZUGFeRD/Factur-X."), Map.entry("bank", "Coordinate bancarie"), Map.entry("invoiceTaxNumberVatId", "Codice fiscale/Partita IVA"), Map.entry("invoicePreviewTitle", "Anteprima fattura"), Map.entry("invoicePreviewHelp", "Anteprima con testi, posizioni, sconti, rate, note e dati bancari."), Map.entry("invoiceNoLines", "Nessuna posizione aggiunta."), Map.entry("invoicePortalTitle", "Portale digitale fatture"), Map.entry("invoicePortalHelp", "Selezionare la lingua desiderata e scaricare nuovamente la fattura."), Map.entry("invoicePortalQrHint", "Aprire questa fattura digitalmente: scansionare il codice QR o aprire il link."), Map.entry("invoiceReadAloud", "Leggi ad alta voce")
  );

  private static final Map<String, String> SV = Map.ofEntries(
    Map.entry("invoice", "Faktura"), Map.entry("proformaInvoice", "Proformafaktura"), Map.entry("credit", "Kreditnota"), Map.entry("cancellation", "Stornofaktura"), Map.entry("paymentAdvice", "Betalningsavisering"), Map.entry("invoiceRecipient", "Fakturamottagare"), Map.entry("invoiceAmount", "Antal"), Map.entry("invoiceDescription", "Beskrivning"), Map.entry("invoiceTaxRate", "Moms"), Map.entry("invoiceSinglePrice", "Styckpris"), Map.entry("invoiceTotalPrice", "Totalpris"), Map.entry("invoiceProductCode", "Kod"), Map.entry("invoiceDate", "Fakturadatum"), Map.entry("treatmentDate", "Behandlingsdatum"), Map.entry("dueDate", "Förfallodatum"), Map.entry("invoiceCustomerFile", "Kundfil"), Map.entry("invoiceUser", "Användare"), Map.entry("invoicePaymentMethod", "Betalningssätt"), Map.entry("invoiceSalutationLabel0", "Hej <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>,"), Map.entry("invoiceInvoiceTextLabel0", "vi var glada att behandla <SieIhrKind>, <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>, den <Behandlungsdatum> och hoppas att vistelsen hos oss var trevlig. -br- Härmed fakturerar vi behandlingen enligt följande:"), Map.entry("invoiceLawHintLabel0", "Vi svarar gärna på ytterligare frågor. -br- Denna faktura har skapats elektroniskt och är giltig utan underskrift eller stämpel."), Map.entry("invoiceGreetingsLabel0", "Med vänliga hälsningar -br--br- Ditt <Gesellschaftsname>"), Map.entry("invoiceYou", "dig"), Map.entry("net", "Netto"), Map.entry("vat", "Moms"), Map.entry("gross", "Totalt"), Map.entry("invoiceReducement", "Rabatt"), Map.entry("invoiceReducedTotal", "Total efter avdrag"), Map.entry("invoiceCoupon", "Kupong"), Map.entry("invoiceInstallments", "Delbetalning"), Map.entry("invoiceInstallmentApprox", "delbetalningar om ca"), Map.entry("invoiceNoRecipient", "Ingen .lbd-mottagare hittades - konfigurera."), Map.entry("bank", "Bankuppgifter"), Map.entry("invoiceTaxNumberVatId", "Skatte-/momsnummer"), Map.entry("invoicePreviewTitle", "Fakturaförhandsvisning"), Map.entry("invoicePreviewHelp", "Förhandsvisning med texter, poster, rabatter, delbetalningar, notiser och bankuppgifter."), Map.entry("invoiceNoLines", "Inga poster har lagts till."), Map.entry("invoicePortalTitle", "Digital fakturaportal"), Map.entry("invoicePortalHelp", "Välj önskat språk och ladda ner fakturan igen."), Map.entry("invoicePortalQrHint", "Öppna fakturan digitalt: skanna QR-koden eller öppna länken."), Map.entry("invoiceReadAloud", "Läs upp")
  );

  private static final Map<String, String> TR = Map.ofEntries(
    Map.entry("invoice", "Fatura"), Map.entry("proformaInvoice", "Proforma fatura"), Map.entry("credit", "Alacak dekontu"), Map.entry("cancellation", "İptal faturası"), Map.entry("paymentAdvice", "Ödeme bildirimi"), Map.entry("invoiceRecipient", "Fatura alıcısı"), Map.entry("invoiceAmount", "Miktar"), Map.entry("invoiceDescription", "Açıklama"), Map.entry("invoiceTaxRate", "KDV oranı"), Map.entry("invoiceSinglePrice", "Birim fiyat"), Map.entry("invoiceTotalPrice", "Toplam fiyat"), Map.entry("invoiceProductCode", "Kod"), Map.entry("invoiceDate", "Fatura tarihi"), Map.entry("treatmentDate", "Tedavi tarihi"), Map.entry("dueDate", "Vade tarihi"), Map.entry("invoiceCustomerFile", "Müşteri dosyası"), Map.entry("invoiceUser", "Kullanıcı"), Map.entry("invoicePaymentMethod", "Ödeme yöntemi"), Map.entry("invoiceSalutationLabel0", "Sayın <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>,"), Map.entry("invoiceInvoiceTextLabel0", "<SieIhrKind>, <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname> adlı kişiyi <Behandlungsdatum> tarihinde tedavi etmekten memnuniyet duyduk ve bizdeki ziyaretinizin hoş geçtiğini umarız. -br- Tedaviyi aşağıdaki şekilde faturalandırıyoruz:"), Map.entry("invoiceLawHintLabel0", "Başka sorularınız olursa memnuniyetle yanıtlarız. -br- Bu fatura elektronik olarak oluşturulmuştur ve imza ya da kaşe olmadan geçerlidir."), Map.entry("invoiceGreetingsLabel0", "Saygılarımızla -br--br- <Gesellschaftsname>"), Map.entry("invoiceYou", "sizi"), Map.entry("net", "Net"), Map.entry("vat", "KDV"), Map.entry("gross", "Toplam"), Map.entry("invoiceReducement", "İndirim"), Map.entry("invoiceReducedTotal", "İndirim sonrası toplam"), Map.entry("invoiceCoupon", "Kupon"), Map.entry("invoiceInstallments", "Taksitli ödeme"), Map.entry("invoiceInstallmentApprox", "yaklaşık taksit"), Map.entry("invoiceNoRecipient", ".lbd alıcısı bulunamadı - lütfen yapılandırın."), Map.entry("bank", "Banka bilgileri"), Map.entry("invoiceTaxNumberVatId", "Vergi/KDV no."), Map.entry("invoicePreviewTitle", "Fatura önizlemesi"), Map.entry("invoicePreviewHelp", "Metinler, kalemler, indirimler, taksitler, notlar ve banka bilgileriyle önizleme."), Map.entry("invoiceNoLines", "Henüz kalem eklenmedi."), Map.entry("invoicePortalTitle", "Dijital fatura portalı"), Map.entry("invoicePortalHelp", "Tercih ettiğiniz dili seçin ve faturayı tekrar indirin."), Map.entry("invoicePortalQrHint", "Bu faturayı dijital olarak açın: QR kodunu tarayın veya bağlantıyı açın."), Map.entry("invoiceReadAloud", "Sesli oku")
  );

  private static final Map<String, String> RU = Map.ofEntries(
    Map.entry("invoice", "Счёт"), Map.entry("proformaInvoice", "Проформа-счёт"), Map.entry("credit", "Кредит-нота"), Map.entry("cancellation", "Сторнировочный счёт"), Map.entry("paymentAdvice", "Платёжное уведомление"), Map.entry("invoiceRecipient", "Получатель счёта"), Map.entry("invoiceAmount", "Количество"), Map.entry("invoiceDescription", "Описание"), Map.entry("invoiceTaxRate", "Ставка НДС"), Map.entry("invoiceSinglePrice", "Цена за единицу"), Map.entry("invoiceTotalPrice", "Итого"), Map.entry("invoiceProductCode", "Код"), Map.entry("invoiceDate", "Дата счёта"), Map.entry("treatmentDate", "Дата лечения"), Map.entry("dueDate", "Срок оплаты"), Map.entry("invoiceCustomerFile", "Файл клиента"), Map.entry("invoiceUser", "Пользователь"), Map.entry("invoicePaymentMethod", "Способ оплаты"), Map.entry("invoiceSalutationLabel0", "Уважаемый(ая) <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>,"), Map.entry("invoiceInvoiceTextLabel0", "мы были рады лечить <SieIhrKind>, <Anrede> <Titel> <Vorname> <Namenszusatz> <Nachname>, <Behandlungsdatum> и надеемся, что ваше пребывание у нас было приятным. -br- Настоящим выставляем счёт за лечение следующим образом:"), Map.entry("invoiceLawHintLabel0", "Мы с удовольствием ответим на ваши дополнительные вопросы. -br- Этот счёт создан электронно и действителен без подписи или печати."), Map.entry("invoiceGreetingsLabel0", "С уважением -br--br- Ваш <Gesellschaftsname>"), Map.entry("invoiceYou", "Вас"), Map.entry("net", "Нетто"), Map.entry("vat", "НДС"), Map.entry("gross", "Итого"), Map.entry("invoiceReducement", "Скидка"), Map.entry("invoiceReducedTotal", "Итого после вычета"), Map.entry("invoiceCoupon", "Купон"), Map.entry("invoiceInstallments", "Оплата частями"), Map.entry("invoiceInstallmentApprox", "платежей примерно по"), Map.entry("invoiceNoRecipient", "Получатель .lbd не найден — настройте."), Map.entry("bank", "Банковские реквизиты"), Map.entry("invoiceTaxNumberVatId", "Налоговый номер/НДС"), Map.entry("invoicePreviewTitle", "Предпросмотр счёта"), Map.entry("invoicePreviewHelp", "Предпросмотр с текстами, позициями, скидками, рассрочкой, примечаниями и банковскими данными."), Map.entry("invoiceNoLines", "Позиции ещё не добавлены."), Map.entry("invoicePortalTitle", "Цифровой портал счетов"), Map.entry("invoicePortalHelp", "Выберите нужный язык и скачайте счёт снова."), Map.entry("invoicePortalQrHint", "Откройте этот счёт цифровым способом: отсканируйте QR-код или откройте ссылку."), Map.entry("invoiceReadAloud", "Прочитать вслух")
  );

}
