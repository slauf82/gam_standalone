package de.kopfzentrum.gam.invoice;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import org.springframework.core.io.ClassPathResource;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentDocumentService {
  private final InvoiceRepository invoices;
  private final JdbcTemplate jdbc;
  private final PaymentWorkflowSettingsRepository paymentSettings;
  private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.GERMANY);

  public PaymentDocumentService(InvoiceRepository invoices, JdbcTemplate jdbc, PaymentWorkflowSettingsRepository paymentSettings) {
    this.invoices = invoices;
    this.jdbc = jdbc;
    this.paymentSettings = paymentSettings;
  }

  public byte[] render(String invoiceNumber, Integer companyId, String type, String language,
                       BigDecimal openAmount, LocalDate dueDate) {
    try {
      InvoiceDetail detail = invoices.findDetail(invoiceNumber, companyId);
      InvoiceSummary summary = detail.summary();
      InvoiceCompany company = invoices.findCompany(summary.companyId());
      InvoiceDocumentData documentData = invoices.findDocumentData(summary.number(), summary.companyId());
      LbdRecipient recipient = documentData.invoiceRecipient();
      String lang = normalize(language);
      String title = title(type, lang);
      int termDays = Math.max(1, paymentSettings.load().paymentTermDays());
      LocalDate effectiveDueDate = dueDate != null ? dueDate : LocalDate.now().plusDays(termDays);
      String body = body(type, lang, invoiceNumber, openAmount, effectiveDueDate);
      String logo = companyLogo(company);
      String recipientHtml = recipientHtml(recipient) + roleReferenceHtml(documentData, lang);
      String html = """
        <!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml" lang="%s" xml:lang="%s"><head>
        <meta charset="UTF-8"/><title>%s</title><meta name="description" content="%s"/><meta name="dc.description" content="%s"/><style>
        @page{size:A4;margin:22mm 18mm}body{font-family:Arial,sans-serif;font-size:11pt;line-height:1.5;color:#222}
        .header{display:table;width:100%%}.left,.right{display:table-cell;vertical-align:top}.right{text-align:right}
        .logo{max-width:180px;max-height:70px;width:auto;height:auto}.recipient{margin:18mm 0 10mm}
        .document-title{font-size:20pt;font-weight:bold;margin:0}.box{border:1px solid #aaa;background:#f7f7f7;padding:12px;margin:8mm 0}.amount{font-size:14pt;font-weight:bold}
        .footer{margin-top:18mm;font-size:9pt}.portal-note{margin-top:10mm;font-size:9pt}.recipient p,.company-address p{margin:0}.company-name{font-weight:normal;font-size:11pt}
        </style></head><body>
        <div class="header"><div class="left">%s<div class="company-address"><p class="company-name">%s</p>%s</div></div><div class="right"><h1 class="document-title">%s</h1></div></div>
        <div class="recipient">%s</div>
        <p>%s</p><div class="box"><p>%s</p><p class="amount">%s</p><p>%s: %s</p></div>
        <p>%s</p><p>%s</p>
        <p class="portal-note">%s</p>
        <div class="footer">%s</div></body></html>
        """.formatted(lang,lang,esc(title),esc(documentDescription(type, lang, invoiceNumber)),esc(documentDescription(type, lang, invoiceNumber)),logo,esc(company==null?"":company.name()),companyAddress(company),esc(title),recipientHtml,
          esc(greeting(lang, recipient)),esc(body),esc(EUR.format(openAmount==null?BigDecimal.ZERO:openAmount)),esc(labelDue(lang)),esc(formatDate(effectiveDueDate, lang)),
          esc(paymentRequest(lang)),esc(closing(lang)),esc(portalHint(lang)),"");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      OpenHtmlPdfLogging.configure();
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.usePdfUaAccessibility(true);
      builder.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_3_U);
      builder.useColorProfile(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData());
      registerWindowsFonts(builder);
      if (html == null || html.isBlank() || !html.contains(title)) throw new IllegalStateException("Dokumentinhalt ist leer");
      builder.withHtmlContent(html, "file:/");
      builder.toStream(out);
      builder.run();
      return InvoicePdfAccessibility.finalizePdfUaMetadata(
          out.toByteArray(),
          title,
          lang,
          company == null || company.name() == null || company.name().isBlank()
              ? "GAM 2.0 Standalone"
              : company.name());
    } catch (Exception e) {
      throw new IllegalStateException("Barrierefreies Zahlungsdokument konnte nicht erzeugt werden: " + e.getMessage(), e);
    }
  }


  private static String roleReferenceHtml(InvoiceDocumentData data, String language) {
    if (data == null || data.treatedPerson() == null || !data.treatedPerson().found()
        || data.treatedPerson() == data.invoiceRecipient()) return "";
    String label = switch (normalize(language)) {
      case "en" -> data.companyCase() ? "Cost coverage for" : "Services for";
      case "fr" -> data.companyCase() ? "Prise en charge pour" : "Prestations pour";
      case "uk" -> data.companyCase() ? "Покриття витрат для" : "Послуги для";
      case "it" -> data.companyCase() ? "Copertura dei costi per" : "Prestazioni per";
      case "sv" -> data.companyCase() ? "Kostnadstäckning för" : "Tjänster för";
      case "tr" -> data.companyCase() ? "Masraf karşılaması" : "Hizmetler";
      case "ru" -> data.companyCase() ? "Оплата расходов за" : "Услуги для";
      case "es" -> data.companyCase() ? "Cobertura de costes para" : "Servicios para";
      case "pt" -> data.companyCase() ? "Cobertura de custos para" : "Serviços para";
      case "nl" -> data.companyCase() ? "Kostenovername voor" : "Diensten voor";
      case "pl" -> data.companyCase() ? "Pokrycie kosztów dla" : "Usługi dla";
      case "cs" -> data.companyCase() ? "Úhrada nákladů pro" : "Služby pro";
      default -> data.companyCase() ? "Kostenübernahme für" : "Leistungen für";
    };
    return "<p><b>" + esc(label) + ":</b> " + esc(joinNonBlank(data.treatedPerson().title(), data.treatedPerson().firstName(), data.treatedPerson().nameSuffix(), data.treatedPerson().lastName())) + "</p>";
  }

  private static String recipientHtml(LbdRecipient recipient) {
    if (recipient == null || !recipient.found()) return "";
    String name = joinNonBlank(
        cleanRecipientValue(recipient.salutation()),
        cleanRecipientValue(recipient.title()),
        cleanRecipientValue(recipient.firstName()),
        cleanRecipientValue(recipient.nameSuffix()),
        cleanRecipientValue(recipient.lastName()));
    String city = joinNonBlank(cleanRecipientValue(recipient.postalCode()), cleanRecipientValue(recipient.city()));
    return java.util.stream.Stream.of(name, cleanRecipientValue(recipient.street()), city, cleanRecipientValue(recipient.country()))
        .filter(v -> v != null && !v.isBlank())
        .map(v -> "<p>" + esc(v) + "</p>")
        .reduce(String::concat)
        .orElse("");
  }

  private static String joinNonBlank(String... values) {
    return java.util.Arrays.stream(values)
        .filter(v -> v != null && !v.isBlank())
        .reduce((a, b) -> a + " " + b)
        .orElse("");
  }

  private static String cleanRecipientValue(String value) {
    if (value == null) return "";
    String v = value.trim();
    if (v.matches("(?i)^b['\"](?:0|1|true|false)['\"]$")) return "";
    if (v.matches("(?i)^b['\"].*['\"]$")) {
      v = v.substring(2, v.length() - 1).trim();
    }
    return v;
  }

  private static String companyLogo(InvoiceCompany company) {
    try {
      String logo = companyLogoFile(company);
      if (logo == null || logo.isBlank()) return "";
      if (logo.startsWith("http://") || logo.startsWith("https://")) {
        return "<div class='logo' role='img' aria-label='Gesellschaftslogo' style=\"background-image:url('" + esc(logo) + "')\"></div>";
      }
      String path = logo.startsWith("/images/") ? logo.substring("/images/".length()) : logo;
      if (path.startsWith("images/")) path = path.substring("images/".length());
      ClassPathResource res = new ClassPathResource("static/images/" + path);
      if (!res.exists()) return "";
      byte[] data = res.getInputStream().readAllBytes();
      String lower = path.toLowerCase(Locale.ROOT);
      String mime = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? "image/jpeg"
          : lower.endsWith(".webp") ? "image/webp"
          : lower.endsWith(".gif") ? "image/gif" : "image/png";
      return "<div class='logo' role='img' aria-label='Gesellschaftslogo' style=\"background-image:url('data:"
          + mime + ";base64," + Base64.getEncoder().encodeToString(data) + "')\"></div>";
    } catch (Exception ignored) {
      return "";
    }
  }

  private static String companyLogoFile(InvoiceCompany company) {
    if (company != null && company.logoId() != null && company.logoId() > 0
        && company.logoUrl() != null && !company.logoUrl().isBlank()) {
      return company.logoUrl();
    }
    int id = company == null || company.id() == null ? -1 : company.id();
    return switch (id) {
      case 1 -> "logo_AMAE_blau.png";
      case 2, 3 -> "logo_ACQUA_blau.png";
      case 6 -> "Healthcode_logo_blau.png";
      default -> "KOPFZENTRUM_LOGO.png";
    };
  }

  private static void registerWindowsFonts(PdfRendererBuilder builder) {
    registerFont(builder, "C:/Windows/Fonts/arial.ttf", "Arial");
    registerFont(builder, "C:/Windows/Fonts/arialbd.ttf", "Arial");
    registerFont(builder, "C:/Windows/Fonts/ariali.ttf", "Arial");
    registerFont(builder, "C:/Windows/Fonts/arialbi.ttf", "Arial");
    registerFont(builder, "C:/Windows/Fonts/segoeui.ttf", "Segoe UI");
  }

  private static void registerFont(PdfRendererBuilder builder, String path, String family) {
    File file = new File(path);
    if (file.isFile()) builder.useFont(file, family);
  }

  private static String formatDate(LocalDate date, String lang) {
    if (date == null) return "";
    Locale locale = localeFor(lang);
    return date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale));
  }

  private static String documentDescription(String type, String lang, String invoiceNumber) {
    Texts t = texts(lang);
    return t.description().replace("{title}", title(type, lang)).replace("{invoice}", invoiceNumber);
  }

  private static String companyAddress(InvoiceCompany c) {
    if (c == null) return "";
    return java.util.stream.Stream.of(c.street(), c.city())
        .filter(v -> v != null && !v.isBlank())
        .map(v -> "<p>" + esc(v) + "</p>")
        .reduce(String::concat)
        .orElse("");
  }

  static String localizedTitle(String type, String language) {
    return title(type, normalize(language));
  }

  private static String title(String type, String lang) {
    Texts t = texts(lang);
    return switch (type) {
      case "REMINDER" -> t.reminder();
      case "DUNNING_1" -> t.dunning1();
      case "DUNNING_2" -> t.dunning2();
      case "DUNNING_3" -> t.dunning3();
      case "COLLECTION" -> t.collection();
      default -> t.paymentDocument();
    };
  }

  private static String body(String type, String lang, String number, BigDecimal amount, LocalDate due) {
    return texts(lang).body().replace("{invoice}", number);
  }

  private static String greeting(String lang, LbdRecipient recipient) {
    String first = recipient == null ? "" : cleanRecipientValue(recipient.firstName());
    String last = recipient == null ? "" : cleanRecipientValue(recipient.lastName());
    String title = recipient == null ? "" : cleanRecipientValue(recipient.title());
    String salutation = recipient == null ? "" : cleanRecipientValue(recipient.salutation());
    Integer salutationIndex = recipient == null ? null : recipient.salutationIndex();
    String sal = salutation.toLowerCase(Locale.ROOT);
    String full = joinNonBlank(title, first, last);
    String formalLastName = joinNonBlank(title, last);
    boolean female = Integer.valueOf(2).equals(salutationIndex) || sal.equals("2") || sal.equals("b'2'") || sal.equals("b\"2\"") || sal.contains("frau") || sal.contains("mrs") || sal.contains("ms");
    boolean male = Integer.valueOf(1).equals(salutationIndex) || sal.equals("1") || sal.equals("b'1'") || sal.equals("b\"1\"") || sal.contains("herr") || sal.equals("mr") || sal.startsWith("mr ");
    Texts t = texts(lang);
    if (!last.isBlank() && female) return t.greetingFemale().replace("{name}", formalLastName);
    if (!last.isBlank() && male) return t.greetingMale().replace("{name}", formalLastName);
    return full.isBlank() ? t.greetingNeutral() : t.greetingNamed().replace("{name}", full);
  }

  private static String labelDue(String lang) { return texts(lang).dueDate(); }
  private static String paymentRequest(String lang) { return texts(lang).paymentRequest(); }
  private static String closing(String lang) { return texts(lang).closing(); }
  private static String portalHint(String lang) { return texts(lang).portalHint(); }
  private static String accessibilityNote(String lang) { return texts(lang).accessibility(); }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) return "de";
    String l = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    String base = l.contains("-") ? l.substring(0, l.indexOf('-')) : l;
    return switch (base) {
      case "de", "en", "fr", "uk", "it", "sv", "tr", "ru", "es", "pt", "nl", "pl", "cs" -> base;
      default -> "de";
    };
  }

  private static Locale localeFor(String lang) {
    return switch (normalize(lang)) {
      case "en" -> Locale.US; case "fr" -> Locale.FRANCE; case "uk" -> Locale.forLanguageTag("uk-UA");
      case "it" -> Locale.ITALY; case "sv" -> Locale.forLanguageTag("sv-SE"); case "tr" -> Locale.forLanguageTag("tr-TR");
      case "ru" -> Locale.forLanguageTag("ru-RU"); case "es" -> Locale.forLanguageTag("es-ES"); case "pt" -> Locale.forLanguageTag("pt-PT");
      case "nl" -> Locale.forLanguageTag("nl-NL"); case "pl" -> Locale.forLanguageTag("pl-PL"); case "cs" -> Locale.forLanguageTag("cs-CZ");
      default -> Locale.GERMANY;
    };
  }

  private static Texts texts(String language) {
    return switch (normalize(language)) {
      case "en" -> new Texts("Payment reminder","First reminder","Second reminder","Final reminder","Debt collection notice","Payment document","The amount for invoice {invoice} is still outstanding.","Due date","Please transfer the outstanding amount promptly, quoting the invoice number.","If payment has already been made, please disregard this document.","This document is also available in the patient portal and can be read aloud there.","Dear Ms {name},","Dear Mr {name},","Dear Sir or Madam,","Dear {name},","{title} for invoice {invoice}","Accessible PDF/UA-oriented document generated by GAM2.");
      case "fr" -> new Texts("Rappel de paiement","Première relance","Deuxième relance","Dernière relance","Avis de recouvrement","Document de paiement","Le montant de la facture {invoice} reste impayé.","Date d’échéance","Veuillez régler rapidement le montant restant en indiquant le numéro de facture.","Si le paiement a déjà été effectué, veuillez ignorer ce document.","Ce document est également disponible dans le portail patient et peut y être lu à voix haute.","Madame {name},","Monsieur {name},","Madame, Monsieur,","Bonjour {name},","{title} pour la facture {invoice}","Document accessible orienté PDF/UA généré par GAM2.");
      case "uk" -> new Texts("Нагадування про оплату","Перше нагадування","Друге нагадування","Останнє нагадування","Повідомлення про стягнення","Платіжний документ","Сума за рахунком {invoice} залишається несплаченою.","Термін оплати","Будь ласка, своєчасно переказуйте несплачену суму із зазначенням номера рахунку.","Якщо оплату вже здійснено, проігноруйте цей документ.","Цей документ також доступний у порталі пацієнта і може бути прочитаний уголос.","Шановна пані {name},","Шановний пане {name},","Шановні пані та панове,","Добрий день, {name},","{title} до рахунку {invoice}","Доступний документ, орієнтований на PDF/UA, створений GAM2.");
      case "it" -> new Texts("Promemoria di pagamento","Primo sollecito","Secondo sollecito","Ultimo sollecito","Avviso di recupero crediti","Documento di pagamento","L’importo della fattura {invoice} risulta ancora insoluto.","Data di scadenza","La preghiamo di versare tempestivamente l’importo residuo indicando il numero della fattura.","Se il pagamento è già stato effettuato, ignori il presente documento.","Questo documento è disponibile anche nel portale paziente e può essere letto ad alta voce.","Gentile Sig.ra {name},","Gentile Sig. {name},","Gentili Signore e Signori,","Gentile {name},","{title} per la fattura {invoice}","Documento accessibile orientato PDF/UA generato da GAM2.");
      case "sv" -> new Texts("Betalningspåminnelse","Första påminnelsen","Andra påminnelsen","Sista påminnelsen","Inkassomeddelande","Betalningsdokument","Beloppet för faktura {invoice} är fortfarande obetalt.","Förfallodatum","Vänligen betala det utestående beloppet snarast och ange fakturanumret.","Om betalning redan har gjorts kan du bortse från detta dokument.","Dokumentet finns även i patientportalen och kan läsas upp där.","Bästa fru {name},","Bästa herr {name},","Bästa mottagare,","Hej {name},","{title} för faktura {invoice}","Tillgängligt PDF/UA-orienterat dokument skapat av GAM2.");
      case "tr" -> new Texts("Ödeme hatırlatması","Birinci ihtar","İkinci ihtar","Son ihtar","Tahsilat bildirimi","Ödeme belgesi","{invoice} numaralı faturaya ait tutar hâlâ ödenmemiştir.","Son ödeme tarihi","Lütfen kalan tutarı fatura numarasını belirterek kısa sürede havale edin.","Ödeme zaten yapıldıysa bu belgeyi dikkate almayın.","Bu belge hasta portalında da mevcuttur ve orada sesli okunabilir.","Sayın Bayan {name},","Sayın Bay {name},","Sayın Yetkili,","Sayın {name},","{invoice} numaralı fatura için {title}","GAM2 tarafından oluşturulan erişilebilir PDF/UA odaklı belge.");
      case "ru" -> new Texts("Напоминание об оплате","Первое напоминание","Второе напоминание","Последнее напоминание","Уведомление о взыскании","Платёжный документ","Сумма по счёту {invoice} всё ещё не оплачена.","Срок оплаты","Просим своевременно перечислить оставшуюся сумму с указанием номера счёта.","Если оплата уже произведена, проигнорируйте этот документ.","Этот документ также доступен в портале пациента и может быть озвучен.","Уважаемая госпожа {name},","Уважаемый господин {name},","Уважаемые дамы и господа,","Здравствуйте, {name},","{title} по счёту {invoice}","Доступный документ, ориентированный на PDF/UA, создан GAM2.");
      case "es" -> new Texts("Recordatorio de pago","Primer requerimiento","Segundo requerimiento","Último requerimiento","Aviso de cobro","Documento de pago","El importe de la factura {invoice} sigue pendiente.","Fecha de vencimiento","Le rogamos que transfiera cuanto antes el importe pendiente indicando el número de factura.","Si ya ha realizado el pago, ignore este documento.","Este documento también está disponible en el portal del paciente y puede leerse en voz alta.","Estimada Sra. {name},","Estimado Sr. {name},","Estimados señores: ","Hola {name},","{title} de la factura {invoice}","Documento accesible orientado a PDF/UA generado por GAM2.");
      case "pt" -> new Texts("Lembrete de pagamento","Primeiro aviso","Segundo aviso","Último aviso","Aviso de cobrança","Documento de pagamento","O montante da fatura {invoice} continua em aberto.","Data de vencimento","Por favor, transfira prontamente o montante em aberto, indicando o número da fatura.","Se o pagamento já tiver sido efetuado, ignore este documento.","Este documento também está disponível no portal do paciente e pode ser lido em voz alta.","Exma. Senhora {name},","Exmo. Senhor {name},","Exmos. Senhores,","Olá {name},","{title} da fatura {invoice}","Documento acessível orientado para PDF/UA gerado pelo GAM2.");
      case "nl" -> new Texts("Betalingsherinnering","Eerste aanmaning","Tweede aanmaning","Laatste aanmaning","Incassobericht","Betalingsdocument","Het bedrag van factuur {invoice} staat nog open.","Vervaldatum","Maak het openstaande bedrag spoedig over onder vermelding van het factuurnummer.","Als de betaling al is verricht, kunt u dit document als niet verzonden beschouwen.","Dit document is ook beschikbaar in het patiëntenportaal en kan daar worden voorgelezen.","Geachte mevrouw {name},","Geachte heer {name},","Geachte heer/mevrouw,","Beste {name},","{title} voor factuur {invoice}","Toegankelijk PDF/UA-georiënteerd document gegenereerd door GAM2.");
      case "pl" -> new Texts("Przypomnienie o płatności","Pierwsze wezwanie","Drugie wezwanie","Ostateczne wezwanie","Informacja o windykacji","Dokument płatniczy","Kwota z faktury {invoice} nadal pozostaje nieuregulowana.","Termin płatności","Prosimy o niezwłoczne przelanie zaległej kwoty z podaniem numeru faktury.","Jeżeli płatność została już dokonana, prosimy zignorować ten dokument.","Dokument jest również dostępny w portalu pacjenta i może zostać odczytany na głos.","Szanowna Pani {name},","Szanowny Panie {name},","Szanowni Państwo,","Dzień dobry {name},","{title} do faktury {invoice}","Dostępny dokument zgodny z założeniami PDF/UA wygenerowany przez GAM2.");
      case "cs" -> new Texts("Připomenutí platby","První upomínka","Druhá upomínka","Poslední upomínka","Oznámení o vymáhání","Platební dokument","Částka za fakturu {invoice} stále nebyla uhrazena.","Datum splatnosti","Uhraďte prosím dlužnou částku co nejdříve a uveďte číslo faktury.","Pokud již byla platba provedena, tento dokument ignorujte.","Tento dokument je k dispozici také v pacientském portálu a lze jej tam nechat přečíst.","Vážená paní {name},","Vážený pane {name},","Vážení,","Dobrý den, {name},","{title} k faktuře {invoice}","Přístupný dokument orientovaný na PDF/UA vytvořený systémem GAM2.");
      default -> new Texts("Zahlungserinnerung","1. Mahnung","2. Mahnung","3. Mahnung","Inkasso-/Rechtsanwaltshinweis","Zahlungsdokument","Für die Rechnung {invoice} ist der folgende Betrag weiterhin offen.","Fälligkeitsdatum","Bitte überweisen Sie den offenen Betrag zeitnah unter Angabe der Rechnungsnummer.","Sollten Sie bereits gezahlt haben, betrachten Sie dieses Schreiben bitte als gegenstandslos.","Dieses Dokument steht auch im Patientenportal zur Verfügung und kann dort vorgelesen werden.","Sehr geehrte Frau {name},","Sehr geehrter Herr {name},","Sehr geehrte Damen und Herren,","Guten Tag {name},","{title} zur Rechnung {invoice}","Barrierefreies, PDF/UA-orientiertes Dokument – erzeugt durch GAM2.");
    };
  }

  private record Texts(String reminder, String dunning1, String dunning2, String dunning3, String collection,
      String paymentDocument, String body, String dueDate, String paymentRequest, String closing, String portalHint,
      String greetingFemale, String greetingMale, String greetingNeutral, String greetingNamed,
      String description, String accessibility) {}

  private static String esc(Object o){return o==null?"":String.valueOf(o).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
