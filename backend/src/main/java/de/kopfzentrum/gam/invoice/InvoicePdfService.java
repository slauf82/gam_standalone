package de.kopfzentrum.gam.invoice;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfString;
import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import de.kopfzentrum.gam.invoice.lbd.LbdService;
import de.kopfzentrum.gam.translation.UiTranslationService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InvoicePdfService {
  private final InvoiceRepository repo;
  private final LbdService lbdService;
  private final TranslationService translations;
  private final UiTranslationService liveTranslations;
  private final InvoiceAccessTokenRepository accessTokens;
  private final QrCodeService qrCodeService;
  private final String portalBaseUrl;
  private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.GERMANY);

  public InvoicePdfService(InvoiceRepository repo, LbdService lbdService, TranslationService translations, UiTranslationService liveTranslations, InvoiceAccessTokenRepository accessTokens, QrCodeService qrCodeService, @Value("${app.invoice.portal.public-base-url:http://localhost:8080/api/invoice-portal}") String portalBaseUrl) {
    this.repo = repo; this.lbdService = lbdService; this.translations = translations; this.liveTranslations = liveTranslations; this.accessTokens = accessTokens; this.qrCodeService = qrCodeService; this.portalBaseUrl = portalBaseUrl;
  }

  /** Normal-PDF bleibt Fallback/Debug. Der verbindliche Export läuft über ZUGFeRD/Factur-X. */
  public byte[] render(String number) { return renderVisualPdf(number, null, false, "de"); }
  public byte[] render(String number, String language) { return renderVisualPdf(number, null, false, language); }
  public byte[] render(String number, Integer companyId, String language) { return renderVisualPdf(number, companyId, false, language); }

  public byte[] renderVisualPdf(String number, boolean forZugferd) { return renderVisualPdf(number, null, forZugferd, "de"); }
  public byte[] renderVisualPdf(String number, boolean forZugferd, String language) { return renderVisualPdf(number, null, forZugferd, language); }

  /**
   * Verbindlicher Renderer für Vorschau/PDF/ZUGFeRD.
   * Wichtig: Rechnungsnummern sind in Alt-GAM nicht zwingend global eindeutig.
   * Deshalb muss die Gesellschaft bei PDF/ZUGFeRD bis in den Renderer durchgereicht werden.
   */
  public byte[] renderVisualPdf(String number, Integer companyId, boolean forZugferd, String language) {
    InvoiceDetail detail = repo.findDetail(number, companyId);
    InvoiceSummary summary = detail.summary();
    List<InvoiceLine> lines = detail.lines();
    InvoiceTotals totals = detail.totals() == null ? repo.calculateFromExistingLines(lines) : detail.totals();
    InvoiceCompany company = repo.findCompany(summary.companyId());
    LbdRecipient recipient = repo.findInvoiceRecipient(summary.number(), summary.companyId());
    if (recipient == null || !recipient.found()) {
      try { recipient = lbdService.preview(""); } catch (Exception e) { recipient = null; }
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 45, 45, 42, 45);
    PdfWriter writer = PdfWriter.getInstance(document, out);
    writer.setViewerPreferences(PdfWriter.DisplayDocTitle);
    document.addTitle(documentTitle(summary.number(), language) + " " + summary.number());
    document.addSubject("GAM 2.0 accessible invoice document");
    document.addCreator("GAM 2.0");
    document.addAuthor(company == null ? "GAM 2.0" : nullSafe(company.name()));
    document.addKeywords("invoice, accessibility, PDF/UA, " + languageTag(language));
    // Schritt 36g: PDF/UA-Grundlagen. OpenPDF erzeugt hier Metadaten, Sprache und konsistente Lesereihenfolge;
    // vollstaendige PDF/UA-Validierung bleibt abhaengig von der eingesetzten PDF-Bibliothek.
    try { writer.getExtraCatalog().put(PdfName.LANG, new PdfString(languageTag(language))); } catch (Exception ignored) { }
    document.open();

    Font title = new Font(Font.HELVETICA, 18, Font.BOLD);
    Font sub = new Font(Font.HELVETICA, 8, Font.NORMAL);
    Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
    Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);
    Font small = new Font(Font.HELVETICA, 8, Font.NORMAL);

    PdfPTable head = new PdfPTable(new float[]{6.5f, 3.5f});
    head.setWidthPercentage(100);
    PdfPCell left = companyLogoCell(company, small);
    PdfPCell right = borderless(new Phrase(documentTitle(summary.number(), language) + " " + summary.number(), title));
    right.setHorizontalAlignment(Element.ALIGN_RIGHT);
    head.addCell(left); head.addCell(right);
    document.add(head);
    document.add(new Paragraph(" "));

    document.add(new Paragraph(senderLine(company), sub));
    document.add(new Paragraph(translations.invoice("invoiceRecipient", language), bold));
    if (recipient != null && recipient.found()) {
      document.add(new Paragraph(recipientName(recipient), normal));
      document.add(new Paragraph(nullSafe(recipient.street()), normal));
      document.add(new Paragraph((nullSafe(recipient.postalCode()) + " " + nullSafe(recipient.city())).trim(), normal));
      if (recipient.country() != null && !recipient.country().isBlank()) document.add(new Paragraph(recipient.country(), normal));
    } else {
      document.add(new Paragraph(translations.invoice("invoiceNoRecipient", language), normal));
    }
    document.add(new Paragraph(" "));

    document.add(new Paragraph(invoiceText("invoiceSalutationLabel0", language, summary, company, recipient), normal));
    document.add(new Paragraph(invoiceText("invoiceInvoiceTextLabel0", language, summary, company, recipient), normal));
    document.add(new Paragraph(" "));

    PdfPTable meta = new PdfPTable(new float[]{6f, 4f});
    meta.setWidthPercentage(100);
    meta.addCell(borderless(new Phrase("", normal)));
    meta.addCell(borderless(new Phrase(translations.invoice("invoiceDate", language) + ": " + formatDate(summary.invoiceDate(), language) + "\n" + translations.invoice("invoiceCustomerFile", language) + ": " + (recipient == null ? "—" : nullSafe(recipient.file())) + "\n" + translations.invoice("invoiceUser", language) + ": " + nullSafe(summary.username()), small)));
    document.add(meta);
    document.add(new Paragraph(" "));

    PdfPTable table = new PdfPTable(new float[]{1.0f, 1.4f, 5.5f, 1.0f, 1.4f, 1.6f});
    table.setWidthPercentage(100);
    addHeader(table, translations.invoice("invoiceAmount", language)); addHeader(table, translations.invoice("invoiceProductCode", language)); addHeader(table, translations.invoice("invoiceDescription", language)); addHeader(table, translations.invoice("invoiceTaxRate", language)); addHeader(table, translations.invoice("invoiceSinglePrice", language)); addHeader(table, translations.invoice("invoiceTotalPrice", language));
    for (InvoiceLine line : lines) {
      double q = line.quantity() == null ? 1.0 : line.quantity();
      double p = line.price() == null ? 0.0 : line.price();
      addCell(table, trimNumber(q)); addCell(table, nullSafe(line.code())); addCell(table, translatedProductDescription(line, language));
      addCell(table, (line.vat() == null ? 0 : line.vat()) + "%"); addCell(table, EUR.format(p)); addCell(table, EUR.format(q * p));
    }
    document.add(table);
    document.add(new Paragraph(" "));

    addCommercialAdjustments(document, summary, lines, totals, normal, bold, translations, language);

    PdfPTable totalTable = new PdfPTable(new float[]{7f, 3f});
    totalTable.setWidthPercentage(100);
    totalTable.addCell(borderless(new Phrase(forZugferd ? translations.invoice("invoiceZugferdNote", language) : translations.invoice("invoiceFallbackPdfNote", language), small)));
    totalTable.addCell(borderless(new Phrase(translations.invoice("net", language) + ": " + EUR.format(totals.net()) + "\n" + translations.invoice("vat", language) + ": " + EUR.format(totals.vat()) + "\n" + translations.invoice("gross", language) + ": " + EUR.format(totals.gross()), bold)));
    document.add(totalTable);

    document.add(new Paragraph(" "));
    document.add(new Paragraph(invoiceText("invoiceLawHintLabel0", language, summary, company, recipient), small));
    document.add(new Paragraph(invoiceText("invoiceGreetingsLabel0", language, summary, company, recipient), normal));

    if (company != null) {
      document.add(new Paragraph(" "));
      document.add(new Paragraph(translations.invoice("bank", language) + ": " + nullSafe(company.accountHolder()) + " · IBAN " + nullSafe(company.iban()) + " · BIC " + nullSafe(company.bic()), small));
      document.add(new Paragraph(translations.invoice("invoiceTaxNumberVatId", language) + ": " + nullSafe(company.taxNumber()) + " " + nullSafe(company.vatId()), small));
    }

    addInvoicePortalQr(document, summary, language, small);

    document.close();
    return out.toByteArray();
  }

  private void addInvoicePortalQr(Document document, InvoiceSummary summary, String language, Font small) {
    try {
      InvoiceAccessToken access = accessTokens.getOrCreate(summary.number(), summary.companyId());
      String url = portalBaseUrl.replaceAll("/$", "") + "/" + access.token();
      document.add(new Paragraph(" "));
      PdfPTable qrTable = new PdfPTable(new float[]{7f, 3f});
      qrTable.setWidthPercentage(100);
      qrTable.addCell(borderless(new Phrase(translations.invoice("invoicePortalQrHint", language) + "\n" + url, small)));
      Image qr = Image.getInstance(qrCodeService.png(url, 140));
      qr.scaleAbsolute(90, 90);
      PdfPCell qrCell = borderless(new Phrase(""));
      qrCell.addElement(qr);
      qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
      qrTable.addCell(qrCell);
      document.add(qrTable);
    } catch (Exception ignored) { }
  }

  private static void addCommercialAdjustments(Document document, InvoiceSummary summary, List<InvoiceLine> lines, InvoiceTotals totals, Font normal, Font bold, TranslationService translations, String language) {
    try {
      double lineGross = lines.stream().mapToDouble(l -> (l.quantity() == null ? 1.0 : l.quantity()) * (l.price() == null ? 0.0 : l.price())).sum();
      boolean hasRows = false;
      PdfPTable adj = new PdfPTable(new float[]{8f, 2f});
      adj.setWidthPercentage(100);
      if (summary.discountPercent() != null && summary.discountPercent() > 0 && lineGross > 0) {
        adj.addCell(borderless(new Phrase(translations.invoice("invoiceReducement", language) + " " + summary.discountPercent() + "%", normal)));
        adj.addCell(borderless(new Phrase(EUR.format(-(lineGross * summary.discountPercent() / 100.0)), bold)));
        hasRows = true;
      }
      if (summary.couponAmount() != null && summary.couponAmount() > 0) {
        adj.addCell(borderless(new Phrase(summary.discountRemark() == null || summary.discountRemark().isBlank() ? translations.invoice("invoiceCoupon", language) : summary.discountRemark(), normal)));
        adj.addCell(borderless(new Phrase(EUR.format(-summary.couponAmount()), bold)));
        hasRows = true;
      }
      if (hasRows) { document.add(adj); document.add(new Paragraph(" ")); }
      if (summary.installments() != null && summary.installments() > 1) {
        document.add(new Paragraph(translations.invoice("invoiceInstallments", language) + ": " + summary.installments() + " " + translations.invoice("invoiceInstallmentApprox", language) + " " + EUR.format(totals.gross() / summary.installments()), normal));
        document.add(new Paragraph(" "));
      }
    } catch (Exception ignored) { }
  }

  private String invoiceText(String key, String language, InvoiceSummary summary, InvoiceCompany company, LbdRecipient recipient) {
    return replacePlaceholders(translations.invoice(key, language), language, summary, company, recipient)
      .replace("-br-", "\n")
      .replace("  ", " ")
      .trim();
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

    // 1) vorhandene Translation-Tabelle nutzen, aber nichts Neues für Produkttexte persistieren
    String cached = translations.resolve(key, lang, description, false);
    if (usableProductTranslation(cached, description)) return cached;

    // 2) Live-Übersetzung ohne DB-Schreibzugriff erzwingen
    try {
      Map<String, String> live = liveTranslations.translateLive(lang, Map.of(key, description));
      String translated = live == null ? null : live.get(key);
      if (usableProductTranslation(translated, description)) return translated;
    } catch (Exception ignored) { }

    // 3) Niemals deutschen Produkttext in fremdsprachige PDFs schreiben.
    return productTranslationPending(lang);
  }

  private static boolean usableProductTranslation(String value, String source) {
    if (value == null || value.isBlank()) return false;
    return source == null || !value.trim().equalsIgnoreCase(source.trim());
  }

  private static String normalizeLanguage(String language) {
    if (language == null || language.isBlank()) return "de";
    String l = language.trim().toLowerCase(Locale.ROOT);
    if (l.startsWith("en")) return "en";
    if (l.startsWith("fr")) return "fr";
    if (l.startsWith("it")) return "it";
    if (l.startsWith("es")) return "es";
    if (l.startsWith("pt")) return "pt";
    if (l.startsWith("nl")) return "nl";
    if (l.startsWith("pl")) return "pl";
    if (l.startsWith("cs") || l.startsWith("cz")) return "cs";
    if (l.startsWith("sv") || l.startsWith("se")) return "sv";
    if (l.startsWith("tr")) return "tr";
    if (l.startsWith("ru")) return "ru";
    if (l.startsWith("uk") || l.startsWith("ua")) return "uk";
    return "de";
  }

  private static String productTranslationPending(String lang) {
    return switch (lang) {
      case "fr" -> "Traduction de la description du produit en cours";
      case "en" -> "Product description translation pending";
      case "it" -> "Traduzione della descrizione del prodotto in corso";
      case "es" -> "Traducción de la descripción del producto pendiente";
      case "pt" -> "Tradução da descrição do produto pendente";
      case "nl" -> "Vertaling van de productbeschrijving in behandeling";
      case "pl" -> "Tłumaczenie opisu produktu w toku";
      case "cs" -> "Překlad popisu produktu čeká na zpracování";
      case "sv" -> "Översättning av produktbeskrivning pågår";
      case "tr" -> "Ürün açıklaması çevirisi bekleniyor";
      case "ru" -> "Перевод описания продукта ожидается";
      case "uk" -> "Переклад опису продукту очікується";
      default -> "Product description translation pending";
    };
  }

  private static String formatDate(String value, String language) {
    if (value == null || value.isBlank()) return "";
    try {
      String s = value.trim();
      LocalDate d;
      if (s.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
        d = LocalDate.parse(s, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
      } else {
        d = LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
      }
      String lang = language == null ? "de" : language.toLowerCase(Locale.ROOT);
      return switch (lang) {
        case "en", "english" -> d.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        case "fr", "french", "it", "italian" -> d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        case "sv", "swedish" -> d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        default -> d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
      };
    } catch (Exception ignored) {
      return value;
    }
  }

  private static PdfPCell companyLogoCell(InvoiceCompany company, Font font) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(PdfPCell.NO_BORDER);
    cell.setPadding(2);
    try {
      ClassPathResource res = new ClassPathResource("static/images/" + companyLogoFile(company));
      if (res.exists()) {
        Image logo = Image.getInstance(res.getInputStream().readAllBytes());
        logo.scaleToFit(180, 70);
        cell.addElement(logo);
      }
    } catch (Exception ignored) { }
    cell.addElement(companyBlock(company, font));
    return cell;
  }

  private static String companyLogoFile(InvoiceCompany company) {
    if (company == null || company.id() == null) return "KOPFZENTRUM_LOGO.png";
    return switch (company.id()) {
      case 1 -> "logo_AMAE_blau.png";
      case 2, 3 -> "logo_ACQUA_blau.png";
      case 6 -> "Healthcode_logo_blau.png";
      default -> "KOPFZENTRUM_LOGO.png";
    };
  }

  private static Phrase companyBlock(InvoiceCompany company, Font font) {
    if (company == null) return new Phrase("Kopfzentrum\n", font);
    return new Phrase(nullSafe(company.name()) + "\n" + nullSafe(company.street()) + "\n" + nullSafe(company.city()) + "\n" + nullSafe(company.email()), font);
  }
  private static String senderLine(InvoiceCompany c) { return c == null ? "" : nullSafe(c.name()) + " · " + nullSafe(c.street()) + " · " + nullSafe(c.city()); }
  private static String recipientName(LbdRecipient r) { return (nonNull(r.salutation()) + " " + nonNull(r.title()) + " " + nonNull(r.firstName()) + " " + nonNull(r.lastName())).trim(); }
  private static PdfPCell borderless(Phrase p) { PdfPCell c = new PdfPCell(p); c.setBorder(PdfPCell.NO_BORDER); c.setPadding(2); return c; }
  private static void addHeader(PdfPTable table, String text) { PdfPCell c = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 9, Font.BOLD))); c.setPadding(5); table.addCell(c); }
  private static void addCell(PdfPTable table, String text) { PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, new Font(Font.HELVETICA, 9))); c.setPadding(5); table.addCell(c); }
  private static String trimNumber(double d) { return d == Math.rint(d) ? Long.toString(Math.round(d)) : Double.toString(d); }
  private static String nullSafe(String value) { return value == null ? "" : value; }
  private static String nonNull(String value) { return value == null ? "" : value; }
  private static String languageTag(String language) {
    if (language == null) return "de-DE";
    return switch (language.toLowerCase(Locale.ROOT)) {
      case "en", "english" -> "en-US";
      case "fr", "french" -> "fr-FR";
      case "uk", "ukrainian" -> "uk-UA";
      case "it", "italian" -> "it-IT";
      case "sv", "swedish" -> "sv-SE";
      case "tr", "turkish" -> "tr-TR";
      case "ru", "russian" -> "ru-RU";
      default -> "de-DE";
    };
  }

  private String documentTitle(String number, String language) {
    if (number == null) return translations.invoice("invoice", language);
    String n = number.trim();
    if (n.endsWith("S")) return translations.invoice("cancellation", language);
    if (n.endsWith("G")) return translations.invoice("credit", language);
    if (n.endsWith("P")) return translations.invoice("proformaInvoice", language);
    if (n.matches(".*Z\\d*$")) return translations.invoice("paymentAdvice", language);
    return translations.invoice("invoice", language);
  }

}
