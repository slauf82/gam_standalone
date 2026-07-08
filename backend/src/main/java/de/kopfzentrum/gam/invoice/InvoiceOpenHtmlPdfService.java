package de.kopfzentrum.gam.invoice;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import de.kopfzentrum.gam.invoice.lbd.LbdService;
import de.kopfzentrum.gam.translation.UiTranslationService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Base64;

@Service
public class InvoiceOpenHtmlPdfService {
  private final InvoiceRepository repo;
  private final LbdService lbdService;
  private final TranslationService translations;
  private final UiTranslationService liveTranslations;
  private final InvoiceAccessTokenRepository accessTokens;
  private final QrCodeService qrCodeService;
  private final JdbcTemplate jdbc;
  private final String portalBaseUrl;
  private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.GERMANY);

  public InvoiceOpenHtmlPdfService(InvoiceRepository repo, LbdService lbdService, TranslationService translations, UiTranslationService liveTranslations, InvoiceAccessTokenRepository accessTokens, QrCodeService qrCodeService, JdbcTemplate jdbc, @Value("${app.invoice.portal.public-base-url:http://localhost:8080/api/invoice-portal}") String portalBaseUrl) {
    this.repo = repo;
    this.lbdService = lbdService;
    this.translations = translations;
    this.liveTranslations = liveTranslations;
    this.accessTokens = accessTokens;
    this.qrCodeService = qrCodeService;
    this.jdbc = jdbc;
    this.portalBaseUrl = portalBaseUrl;
  }

  /**
   * Schritt 37g: OpenHTMLtoPDF-Basis-PDF als PDF/A-3.
   * Die ZUGFeRD/Factur-X-XML wird anschließend im Controller über den bestehenden
   * MustangProject-Pfad eingebettet.
   */
  public byte[] renderVisualPdf(String number, Integer companyId, String language) {
    try {
      String html = html(number, companyId, language);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      // Schritt 37h3/37h7: PDF/UA-Tagging direkt im OpenHTMLtoPDF-Renderer aktivieren.
      // Wichtig: keine Tabellenstruktur-Umbauten wie in 37h, damit der stabile 37g/37h1-Pfad erhalten bleibt.
      builder.usePdfUaAccessbility(true);
      builder.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_3_U);
      builder.useColorProfile(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData());
      registerWindowsFonts(builder);
      builder.withHtmlContent(html, null);
      builder.toStream(out);
      builder.run();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("OpenHTMLtoPDF-PDF/A-3-Basis-PDF konnte nicht erzeugt werden: " + ex.getMessage(), ex);
    }
  }

  private static void registerWindowsFonts(PdfRendererBuilder builder) {
    registerFont(builder, "C:/Windows/Fonts/arial.ttf", "Arial");
    registerFont(builder, "C:/Windows/Fonts/arialbd.ttf", "Arial");
    registerFont(builder, "C:/Windows/Fonts/ariali.ttf", "Arial");
    registerFont(builder, "C:/Windows/Fonts/arialbi.ttf", "Arial");
    registerFont(builder, "C:/Windows/Fonts/segoeui.ttf", "Segoe UI");
  }

  private static void registerFont(PdfRendererBuilder builder, String path, String family) {
    try {
      File file = new File(path);
      if (file.isFile()) {
        builder.useFont(file, family);
      }
    } catch (Exception ignored) {
      // Font-Registrierung ist optional; OpenHTMLtoPDF nutzt sonst seine Fallbacks.
    }
  }

  private String html(String number, Integer companyId, String language) {
    InvoiceDetail detail = repo.findDetail(number, companyId);
    InvoiceSummary summary = detail.summary();
    List<InvoiceLine> lines = detail.lines();
    InvoiceCompany company = repo.findCompany(summary.companyId());
    LbdRecipient recipient = repo.findInvoiceRecipient(summary.number(), summary.companyId());
    if (recipient == null || !recipient.found()) {
      try { recipient = lbdService.preview(""); } catch (Exception e) { recipient = null; }
    }
    InvoiceTotals totals = detail.totals() == null ? repo.calculateFromExistingLines(lines) : detail.totals();

    String lang = normalizeLanguage(language);
    String title = documentTitle(summary.number(), lang) + " " + escape(summary.number());
    String companyName = company == null ? "" : escape(company.name());
    String companyAddress = companyAddress(company);
    String companyLogo = companyLogoBlock(company);
    String recipientBlock = recipientBlockHtml(recipient, lang);
    String invoiceDate = formatDate(summary.invoiceDate(), lang);
    String customerFile = recipient == null ? "—" : nullSafe(recipient.file());

    StringBuilder rows = new StringBuilder();
    if (lines != null) {
      for (InvoiceLine line : lines) {
        double qty = line.quantity() == null ? 1d : line.quantity();
        double price = line.price() == null ? 0d : line.price();
        double gross = qty * price;
        rows.append("<tr>")
          .append("<td>").append(escape(trimNumber(qty))).append("</td>")
          .append("<td>").append(escape(line.code())).append("</td>")
          .append("<td>").append(escape(translatedProductDescription(line, lang))).append("</td>")
          .append("<td>").append(line.vat() == null ? "0" : line.vat()).append("%</td>")
          .append("<td class='money'>").append(escape(EUR.format(price))).append("</td>")
          .append("<td class='money'>").append(escape(EUR.format(gross))).append("</td>")
          .append("</tr>");
      }
    }

    Integer resolvedCompanyId = summary.companyId() != null ? summary.companyId() : (companyId != null ? companyId : findCompanyIdByInvoice(summary));
    String salutation = invoiceAdminText(resolvedCompanyId, "salutation", "rechnungsanrede", "invoiceSalutationLabel0", lang, summary, company, recipient);
    String invoiceText = invoiceAdminText(resolvedCompanyId, "invoiceText", "rechnungstext", "invoiceInvoiceTextLabel0", lang, summary, company, recipient);
    String lawHint = invoiceAdminText(resolvedCompanyId, "legalNote", "rechnungsrechtlicherhinweis", "invoiceLawHintLabel0", lang, summary, company, recipient);
    String greetings = invoiceAdminText(resolvedCompanyId, "greeting", "rechnungsgrussformel", "invoiceGreetingsLabel0", lang, summary, company, recipient);
    String adjustments = commercialAdjustments(summary, lines, totals, lang);
    String bank = bankBlock(company, lang);
    String portal = portalBlock(summary, lang);

    return """
      <!DOCTYPE html>
      <html xmlns="http://www.w3.org/1999/xhtml" lang="%s" xml:lang="%s">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>%s</title>
        <style>
          @page { size: A4; margin: 22mm 18mm; }
          body { font-family: Arial, Helvetica, sans-serif; font-size: 10.5pt; color: #222; line-height: 1.35; }
          h1 { font-size: 20pt; margin: 0; text-align: right; }
          h2 { font-size: 12.5pt; margin: 7mm 0 2.5mm 0; }
          p { margin: 0 0 3.5mm 0; }
          .company p, .meta p, .recipient p, .total-values p, .bank p, .portal p { margin: 0; }
          .header { display: table; width: 100%%; margin-bottom: 8mm; }
          .header-left { display: table-cell; width: 60%%; vertical-align: top; }
          .header-right { display: table-cell; width: 40%%; vertical-align: top; }
          .logo { margin: 0 0 3mm 0; width: 180px; height: 70px; background-repeat: no-repeat; background-position: left top; background-size: contain; }
          .company { font-size: 11pt; }
          .company-name { font-weight: bold; }
          .sender { font-size: 8pt; margin-bottom: 2mm; }
          .recipient { margin: 1mm 0 8mm 0; }
          .intro { margin: 5mm 0 6mm 0; }
          .meta { float: right; width: 42%%; font-size: 8.5pt; margin: 0 0 5mm 8mm; }
          table { width: 100%%; border-collapse: collapse; margin-top: 5mm; clear: both; }
          th { background: #e6e6e6; text-align: left; font-weight: bold; }
          th, td { border: 1px solid #aaa; padding: 4px 5px; vertical-align: top; }
          .money { text-align: right; white-space: nowrap; }
          .adjustments { margin-top: 5mm; width: 100%%; }
          .adjustments td { border: none; padding: 2px 4px; }
          .totals { margin-top: 5mm; display: table; width: 100%%; }
          .total-note { display: table-cell; width: 65%%; font-size: 8.5pt; vertical-align: top; }
          .total-values { display: table-cell; width: 35%%; font-weight: bold; text-align: right; vertical-align: top; }
          .legal { margin-top: 8mm; font-size: 8.8pt; white-space: pre-line; }
          .greetings { margin-top: 5mm; white-space: pre-line; }
          .bank { margin-top: 7mm; font-size: 8.5pt; }
          .portal { margin-top: 7mm; display: table; width: 100%%; font-size: 8.5pt; page-break-inside: avoid; }
          .portal-text { display: table-cell; width: 72%%; vertical-align: top; }
          .portal-qr { display: table-cell; width: 28%%; text-align: right; vertical-align: top; }
          .portal-qr-image { width: 90px; height: 90px; margin-left: auto; background-repeat: no-repeat; background-position: right top; background-size: 90px 90px; }
          .portal-url { font-size: 7.5pt; overflow-wrap: break-word; }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="header-left">%s<div class="company"><p class="company-name">%s</p>%s</div></div>
          <div class="header-right"><h1>%s</h1></div>
        </div>

        <p class="sender">%s</p>
        <h2>%s</h2>
        <div class="recipient">%s</div>

        <div class="meta"><p>%s: %s</p><p>%s: %s</p></div>
        <div class="intro">
          <p>%s</p>
          <p>%s</p>
        </div>

        <table>
          <thead><tr><th scope="col">%s</th><th scope="col">%s</th><th scope="col">%s</th><th scope="col">%s</th><th scope="col">%s</th><th scope="col">%s</th></tr></thead>
          <tbody>%s</tbody>
        </table>
        %s
        <div class="totals">
          <div class="total-note"><p>%s</p></div>
          <div class="total-values"><p>%s: %s</p><p>%s: %s</p><p>%s: %s</p></div>
        </div>

        <div class="legal">%s</div>
        <div class="greetings">%s</div>
        %s
        %s
      </body>
      </html>
      """.formatted(
        lang, lang, escape(title), companyLogo, companyName, companyAddress, title,
        escape(senderLine(company)), translations.invoice("invoiceRecipient", lang), recipientBlock,
        translations.invoice("invoiceDate", lang), escape(invoiceDate), translations.invoice("invoiceCustomerFile", lang), escape(customerFile),
        xhtmlText(salutation), xhtmlText(invoiceText),
        translations.invoice("invoiceAmount", lang), translations.invoice("invoiceProductCode", lang), translations.invoice("invoiceDescription", lang), translations.invoice("invoiceTaxRate", lang), translations.invoice("invoiceSinglePrice", lang), translations.invoice("invoiceTotalPrice", lang), rows,
        adjustments,
        escape(openHtmlPdfNote(lang)),
        translations.invoice("net", lang), escape(EUR.format(totals == null ? 0d : totals.net())), translations.invoice("vat", lang), escape(EUR.format(totals == null ? 0d : totals.vat())), translations.invoice("gross", lang), escape(EUR.format(totals == null ? 0d : totals.gross())),
        xhtmlBlockText(lawHint), xhtmlBlockText(greetings), bank, portal
      );
  }


  private static String openHtmlPdfNote(String language) {
    return switch (normalizeLanguage(language)) {
      case "en" -> "This invoice contains the machine-readable ZUGFeRD/Factur-X XML in the PDF.";
      case "fr" -> "Cette facture contient le XML ZUGFeRD/Factur-X lisible par machine dans le PDF.";
      case "it" -> "Questa fattura contiene il file XML ZUGFeRD/Factur-X leggibile dalla macchina nel PDF.";
      case "uk" -> "Цей рахунок містить машинозчитуваний XML ZUGFeRD/Factur-X у PDF.";
      default -> "Diese Rechnung enthält die maschinenlesbare ZUGFeRD/Factur-X XML im PDF.";
    };
  }

  private String portalBlock(InvoiceSummary summary, String language) {
    try {
      InvoiceAccessToken access = accessTokens.getOrCreate(summary.number(), summary.companyId());
      String url = portalBaseUrl.replaceAll("/$", "") + "/" + access.token();
      byte[] png = qrCodeService.png(url, 140);
      String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
      String hint = translations.invoice("invoicePortalQrHint", language);
      return "<div class='portal'>"
        + "<div class='portal-text'><p>" + escape(hint) + "</p><p class='portal-url'>" + escape(url) + "</p></div>"
        + "<div class='portal-qr'><div class='portal-qr-image' style=\"background-image:url('" + dataUri + "')\"></div></div>"
        + "</div>";
    } catch (Exception ignored) {
      return "";
    }
  }

  private String commercialAdjustments(InvoiceSummary summary, List<InvoiceLine> lines, InvoiceTotals totals, String language) {
    try {
      double lineGross = lines == null ? 0d : lines.stream().mapToDouble(l -> (l.quantity() == null ? 1.0 : l.quantity()) * (l.price() == null ? 0.0 : l.price())).sum();
      StringBuilder b = new StringBuilder();
      if (summary.discountPercent() != null && summary.discountPercent() > 0 && lineGross > 0) {
        b.append("<tr><td>").append(escape(translations.invoice("invoiceReducement", language))).append(" ").append(summary.discountPercent()).append("%</td><td class='money'>")
          .append(escape(EUR.format(-(lineGross * summary.discountPercent() / 100.0)))).append("</td></tr>");
      }
      if (summary.couponAmount() != null && summary.couponAmount() > 0) {
        String label = summary.discountRemark() == null || summary.discountRemark().isBlank() ? translations.invoice("invoiceCoupon", language) : summary.discountRemark();
        b.append("<tr><td>").append(escape(label)).append("</td><td class='money'>").append(escape(EUR.format(-summary.couponAmount()))).append("</td></tr>");
      }
      if (b.length() == 0 && (summary.installments() == null || summary.installments() <= 1)) return "";
      StringBuilder out = new StringBuilder("<table class='adjustments'><tbody>").append(b).append("</tbody></table>");
      if (summary.installments() != null && summary.installments() > 1) {
        out.append("<p>").append(escape(translations.invoice("invoiceInstallments", language))).append(": ").append(summary.installments()).append(" ")
          .append(escape(translations.invoice("invoiceInstallmentApprox", language))).append(" ")
          .append(escape(EUR.format((totals == null ? 0d : totals.gross()) / summary.installments()))).append("</p>");
      }
      return out.toString();
    } catch (Exception ignored) { return ""; }
  }

  private String bankBlock(InvoiceCompany company, String language) {
    if (company == null) return "";
    String bank = translations.invoice("bank", language) + ": " + nullSafe(company.accountHolder()) + " · IBAN " + nullSafe(company.iban()) + " · BIC " + nullSafe(company.bic());
    String tax = translations.invoice("invoiceTaxNumberVatId", language) + ": " + nullSafe(company.taxNumber()) + " " + nullSafe(company.vatId());
    return "<div class='bank'><p>" + escape(bank) + "</p><p>" + escape(tax) + "</p></div>";
  }

  private String invoiceText(String key, String language, InvoiceSummary summary, InvoiceCompany company, LbdRecipient recipient) {
    return replacePlaceholders(translations.invoice(key, language), language, summary, company, recipient)
      .replace("-br-", "\n")
      .replace("  ", " ")
      .trim();
  }

  private String invoiceAdminText(Integer companyId, String logicalKey, String table, String translationKey, String language, InvoiceSummary summary, InvoiceCompany company, LbdRecipient recipient) {
    String selected = selectedCompanyText(companyId, logicalKey);
    String text = selected == null || selected.isBlank() ? systemFallbackText(table) : selected;
    if (text == null || text.isBlank()) text = hardcodedFallback(logicalKey, translations.invoice(translationKey, language));
    return replacePlaceholders(text, language, summary, company, recipient)
      .replace("-br-", "\n")
      .replace("  ", " ")
      .trim();
  }

  private String selectedCompanyText(Integer companyId, String logicalKey) {
    if (companyId == null) return null;
    String column = switch (logicalKey) {
      case "salutation" -> "a.TEXT";
      case "invoiceText" -> "t.TEXT";
      case "legalNote" -> "h.TEXT";
      case "greeting" -> "g.TEXT";
      default -> null;
    };
    if (column == null) return null;
    try {
      ensureInvoiceTextFallbackRows();
      String sql = """
        SELECT %s
          FROM rechnungstext_gesellschaft_zuordnung z
          LEFT JOIN rechnungsanrede a ON a.ID=z.ANREDE_ID
          LEFT JOIN rechnungstext t ON t.ID=z.RECHNUNGSTEXT_ID
          LEFT JOIN rechnungsrechtlicherhinweis h ON h.ID=z.RECHTLICHER_HINWEIS_ID
          LEFT JOIN rechnungsgrussformel g ON g.ID=z.GRUSSFORMEL_ID
         WHERE z.RGESELLSCHAFTS_ID=?
         LIMIT 1
        """.formatted(column);
      return jdbc.queryForObject(sql, String.class, companyId);
    } catch (Exception ignored) { return null; }
  }

  private String systemFallbackText(String table) {
    try {
      ensureInvoiceTextFallbackRows();
      return jdbc.queryForObject("SELECT `TEXT` FROM `" + table + "` WHERE `ID`=0 LIMIT 1", String.class);
    } catch (Exception ignored) { return null; }
  }

  private void ensureInvoiceTextFallbackRows() {
    try {
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsanrede` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungstext` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsrechtlicherhinweis` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsgrussformel` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("SET SESSION sql_mode = CONCAT_WS(',', @@sql_mode, 'NO_AUTO_VALUE_ON_ZERO')");
      jdbc.update("INSERT IGNORE INTO `rechnungsanrede` (`ID`, `TEXT`) VALUES (0, ?)", "Sehr geehrte Damen und Herren,");
      jdbc.update("INSERT IGNORE INTO `rechnungstext` (`ID`, `TEXT`) VALUES (0, ?)", "Wir erlauben uns folgende Leistungen in Rechnung zu stellen.");
      jdbc.update("INSERT IGNORE INTO `rechnungsrechtlicherhinweis` (`ID`, `TEXT`) VALUES (0, ?)", "Bitte begleichen Sie den Rechnungsbetrag innerhalb der angegebenen Frist.");
      jdbc.update("INSERT IGNORE INTO `rechnungsgrussformel` (`ID`, `TEXT`) VALUES (0, ?)", "Mit freundlichen Grüßen");
    } catch (Exception ignored) { }
  }

  private String hardcodedFallback(String logicalKey, String translationFallback) {
    return switch (logicalKey) {
      case "salutation" -> "Sehr geehrte Damen und Herren,";
      case "invoiceText" -> "Wir erlauben uns folgende Leistungen in Rechnung zu stellen.";
      case "legalNote" -> "Bitte begleichen Sie den Rechnungsbetrag innerhalb der angegebenen Frist.";
      case "greeting" -> "Mit freundlichen Grüßen";
      default -> translationFallback == null ? "" : translationFallback;
    };
  }

  private Integer findCompanyIdByInvoice(InvoiceSummary summary) {
    if (summary == null || summary.number() == null || summary.number().isBlank()) return null;
    try { return jdbc.queryForObject("SELECT RGESELLSCHAFTS_ID FROM rechnungen WHERE NUMMER=? LIMIT 1", Integer.class, summary.number()); }
    catch (Exception ignored) { return null; }
  }

  private String replacePlaceholders(String text, String language, InvoiceSummary summary, InvoiceCompany company, LbdRecipient recipient) {
    if (text == null) return "";
    String result = text;
    result = result.replace("<SieIhrKind>", translations.invoice("invoiceYou", language));
    result = result.replace("<Anrede>", recipient == null ? "" : nullSafe(recipient.salutation()));
    result = result.replace("<Titel>", recipient == null ? "" : nullSafe(recipient.title()));
    result = result.replace("<Vorname>", recipient == null ? "" : nullSafe(recipient.firstName()));
    result = result.replace("<Namenszusatz>", recipient == null ? "" : nullSafe(recipient.nameSuffix()));
    result = result.replace("<Nachname>", recipient == null ? "" : nullSafe(recipient.lastName()));
    result = result.replace("<Behandlungsdatum>", summary == null ? "" : formatDate(summary.invoiceDate(), language));
    result = result.replace("<Gesellschaftsname>", company == null ? "" : nullSafe(company.name()));
    return result;
  }

  private String translatedProductDescription(InvoiceLine line, String language) {
    String description = nullSafe(line.description());
    String code = nullSafe(line.code()).trim();
    String lang = normalizeLanguage(language);
    if (description.isBlank() || "de".equals(lang)) return description;
    String key = code.isBlank() ? "productDescription." + Integer.toHexString(description.hashCode()) : "productDescription." + code.replaceAll("[^A-Za-z0-9_-]", "_");
    String cached = translations.resolve(key, lang, description, false);
    if (usableProductTranslation(cached, description)) return cached;
    try {
      Map<String, String> live = liveTranslations.translateLive(lang, Map.of(key, description));
      String translated = live == null ? null : live.get(key);
      if (usableProductTranslation(translated, description)) return translated;
    } catch (Exception ignored) { }
    return productTranslationPending(lang);
  }

  private static boolean usableProductTranslation(String value, String source) {
    if (value == null || value.isBlank()) return false;
    return source == null || !value.trim().equalsIgnoreCase(source.trim());
  }

  private static String productTranslationPending(String lang) {
    return switch (lang) {
      case "en" -> "Product description translation is being prepared.";
      case "fr" -> "La traduction de la description du produit est en cours de préparation.";
      case "uk" -> "Переклад опису продукту готується.";
      case "it" -> "La traduzione della descrizione del prodotto è in preparazione.";
      default -> "Produktbeschreibung wird übersetzt.";
    };
  }

  private static String companyAddress(InvoiceCompany c) {
    if (c == null) return "";
    StringBuilder b = new StringBuilder();
    if (!blank(c.street())) b.append("<p>").append(escape(c.street())).append("</p>");
    if (!blank(c.city())) b.append("<p>").append(escape(c.city())).append("</p>");
    if (!blank(c.email())) b.append("<p>").append(escape(c.email())).append("</p>");
    return b.toString();
  }

  private static String companyLogoBlock(InvoiceCompany company) {
    try {
      String logo = companyLogoFile(company);
      if (blank(logo)) return "";
      if (logo.startsWith("http://") || logo.startsWith("https://")) {
        return "<div class='logo' style=\"background-image:url('" + escape(logo) + "')\"></div>";
      }
      String path = logo.startsWith("/images/") ? logo.substring("/images/".length()) : logo;
      if (path.startsWith("images/")) path = path.substring("images/".length());
      ClassPathResource res = new ClassPathResource("static/images/" + path);
      if (!res.exists()) return "";
      byte[] data = res.getInputStream().readAllBytes();
      String lower = path.toLowerCase(Locale.ROOT);
      String mime = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? "image/jpeg" : lower.endsWith(".webp") ? "image/webp" : lower.endsWith(".gif") ? "image/gif" : "image/png";
      String uri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(data);
      return "<div class='logo' style=\"background-image:url('" + uri + "')\"></div>";
    } catch (Exception ignored) {
      return "";
    }
  }

  private static String companyLogoFile(InvoiceCompany company) {
    if (company != null && !blank(company.logoUrl())) return company.logoUrl();
    int id = company == null || company.id() == null ? -1 : company.id();
    return switch (id) {
      case 1 -> "logo_AMAE_blau.png";
      case 2, 3 -> "logo_ACQUA_blau.png";
      case 6 -> "Healthcode_logo_blau.png";
      default -> "KOPFZENTRUM_LOGO.png";
    };
  }

  private static String senderLine(InvoiceCompany c) {
    if (c == null) return "";
    StringBuilder b = new StringBuilder();
    if (!blank(c.name())) b.append(c.name());
    if (!blank(c.street())) b.append(" · ").append(c.street());
    if (!blank(c.city())) b.append(" · ").append(c.city());
    return b.toString().replaceFirst("^ · ", "");
  }

  private String recipientBlockHtml(LbdRecipient r, String lang) {
    if (r == null || !r.found()) return "<p>" + escape(translations.invoice("invoiceNoRecipient", lang)) + "</p>";
    StringBuilder b = new StringBuilder();
    String name = recipientName(r);
    if (!blank(name)) b.append("<p>").append(escape(name)).append("</p>");
    if (!blank(r.street())) b.append("<p>").append(escape(r.street())).append("</p>");
    String cityLine = ((r.postalCode() == null ? "" : r.postalCode() + " ") + (r.city() == null ? "" : r.city())).trim();
    if (!blank(cityLine)) b.append("<p>").append(escape(cityLine)).append("</p>");
    if (!blank(r.country()) && !"DE".equalsIgnoreCase(r.country())) b.append("<p>").append(escape(r.country())).append("</p>");
    return b.length() == 0 ? "<p>—</p>" : b.toString();
  }

  private static String recipientName(LbdRecipient r) {
    if (r == null) return "";
    return (nullSafe(r.salutation()) + " " + nullSafe(r.title()) + " " + nullSafe(r.firstName()) + " " + nullSafe(r.nameSuffix()) + " " + nullSafe(r.lastName())).replaceAll("\\s+", " ").trim();
  }

  private String documentTitle(String number, String language) {
    InvoiceDetail detail = repo.findDetail(number, null);
    InvoiceSummary s = detail.summary();
    if (Boolean.TRUE.equals(s.creditNote())) return translations.invoice("credit", language);
    if (Boolean.TRUE.equals(s.cancelled())) return translations.invoice("cancellation", language);
    if (Boolean.TRUE.equals(s.paymentAdvice())) return translations.invoice("paymentAdvice", language);
    return translations.invoice("invoice", language);
  }

  private static String formatDate(String value, String language) {
    if (blank(value)) return "—";
    try {
      String pattern = switch (normalizeLanguage(language)) { case "en" -> "MM/dd/yyyy"; case "fr", "it" -> "dd/MM/yyyy"; default -> "dd.MM.yyyy"; };
      return LocalDate.parse(value).format(DateTimeFormatter.ofPattern(pattern));
    } catch (Exception ignored) { return value; }
  }

  private static String normalizeLanguage(String lang) {
    if (lang == null || lang.isBlank()) return "de";
    String l = lang.toLowerCase(Locale.ROOT);
    return l.length() > 2 ? l.substring(0, 2) : l;
  }

  private static String trimNumber(double value) { return Math.rint(value) == value ? String.format(Locale.GERMANY, "%.0f", value) : String.format(Locale.GERMANY, "%.2f", value); }
  private static boolean blank(String s) { return s == null || s.isBlank(); }
  private static String nullSafe(String s) { return s == null ? "" : s; }

  private static String xhtmlBlockText(String s) {
    if (s == null || s.isBlank()) return "";
    StringBuilder b = new StringBuilder();
    for (String line : s.split("\\R")) {
      String t = line == null ? "" : line.trim();
      if (!t.isBlank()) b.append("<p>").append(escape(t)).append("</p>");
    }
    return b.toString();
  }
  private static String xhtmlText(String s) { return escape(s).replace("\n", "<br />"); }
  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
  }
}
