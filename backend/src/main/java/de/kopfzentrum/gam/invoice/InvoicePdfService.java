package de.kopfzentrum.gam.invoice;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import de.kopfzentrum.gam.invoice.lbd.LbdService;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
public class InvoicePdfService {
  private final InvoiceRepository repo;
  private final LbdService lbdService;
  private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.GERMANY);

  public InvoicePdfService(InvoiceRepository repo, LbdService lbdService) {
    this.repo = repo; this.lbdService = lbdService;
  }

  /** Normal-PDF bleibt Fallback/Debug. Der verbindliche Export läuft über ZUGFeRD/Factur-X. */
  public byte[] render(String number) { return renderVisualPdf(number, false); }

  public byte[] renderVisualPdf(String number, boolean forZugferd) {
    InvoiceDetail detail = repo.findDetail(number);
    InvoiceSummary summary = detail.summary();
    List<InvoiceLine> lines = detail.lines();
    InvoiceTotals totals = detail.totals() == null ? repo.calculateFromExistingLines(lines) : detail.totals();
    InvoiceCompany company = repo.findCompany(summary.companyId());
    LbdRecipient recipient;
    try { recipient = lbdService.preview(""); } catch (Exception e) { recipient = null; }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 45, 45, 42, 45);
    PdfWriter.getInstance(document, out);
    document.open();

    Font title = new Font(Font.HELVETICA, 18, Font.BOLD);
    Font sub = new Font(Font.HELVETICA, 8, Font.NORMAL);
    Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
    Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);
    Font small = new Font(Font.HELVETICA, 8, Font.NORMAL);

    PdfPTable head = new PdfPTable(new float[]{6.5f, 3.5f});
    head.setWidthPercentage(100);
    PdfPCell left = borderless(companyBlock(company, small));
    PdfPCell right = borderless(new Phrase(documentTitle(summary.number()) + " " + summary.number(), title));
    right.setHorizontalAlignment(Element.ALIGN_RIGHT);
    head.addCell(left); head.addCell(right);
    document.add(head);
    document.add(new Paragraph(" "));

    document.add(new Paragraph(senderLine(company), sub));
    document.add(new Paragraph("Rechnungsempfänger", bold));
    if (recipient != null && recipient.found()) {
      document.add(new Paragraph(recipientName(recipient), normal));
      document.add(new Paragraph(nullSafe(recipient.street()), normal));
      document.add(new Paragraph((nullSafe(recipient.postalCode()) + " " + nullSafe(recipient.city())).trim(), normal));
      if (recipient.country() != null && !recipient.country().isBlank()) document.add(new Paragraph(recipient.country(), normal));
    } else {
      document.add(new Paragraph("Kein .lbd-Empfänger gefunden - bitte konfigurieren.", normal));
    }
    document.add(new Paragraph(" "));

    PdfPTable meta = new PdfPTable(new float[]{6f, 4f});
    meta.setWidthPercentage(100);
    meta.addCell(borderless(new Phrase("Vielen Dank. Wir berechnen Ihnen folgende Leistungen:", normal)));
    meta.addCell(borderless(new Phrase("Datum: " + nullSafe(summary.invoiceDate()) + "\nKundendatei: " + (recipient == null ? "—" : nullSafe(recipient.file())) + "\nBenutzer: " + nullSafe(summary.username()), small)));
    document.add(meta);
    document.add(new Paragraph(" "));

    PdfPTable table = new PdfPTable(new float[]{1.0f, 1.4f, 5.5f, 1.0f, 1.4f, 1.6f});
    table.setWidthPercentage(100);
    addHeader(table, "Menge"); addHeader(table, "Code"); addHeader(table, "Beschreibung"); addHeader(table, "MwSt"); addHeader(table, "Einzel"); addHeader(table, "Gesamt");
    for (InvoiceLine line : lines) {
      double q = line.quantity() == null ? 1.0 : line.quantity();
      double p = line.price() == null ? 0.0 : line.price();
      addCell(table, trimNumber(q)); addCell(table, nullSafe(line.code())); addCell(table, nullSafe(line.description()));
      addCell(table, (line.vat() == null ? 0 : line.vat()) + "%"); addCell(table, EUR.format(p)); addCell(table, EUR.format(q * p));
    }
    document.add(table);
    document.add(new Paragraph(" "));

    addCommercialAdjustments(document, summary, lines, totals, normal, bold);

    PdfPTable totalTable = new PdfPTable(new float[]{7f, 3f});
    totalTable.setWidthPercentage(100);
    totalTable.addCell(borderless(new Phrase(forZugferd ? "Diese Rechnung enthält die maschinenlesbare ZUGFeRD/Factur-X XML im PDF." : "Debug-/Fallback-PDF; verbindlich ist der ZUGFeRD/Factur-X-Export.", small)));
    totalTable.addCell(borderless(new Phrase("Netto: " + EUR.format(totals.net()) + "\nMwSt: " + EUR.format(totals.vat()) + "\nGesamt: " + EUR.format(totals.gross()), bold)));
    document.add(totalTable);

    if (company != null) {
      document.add(new Paragraph(" "));
      document.add(new Paragraph("Bankverbindung: " + nullSafe(company.accountHolder()) + " · IBAN " + nullSafe(company.iban()) + " · BIC " + nullSafe(company.bic()), small));
      document.add(new Paragraph("Steuernummer/USt-ID: " + nullSafe(company.taxNumber()) + " " + nullSafe(company.vatId()), small));
    }

    document.close();
    return out.toByteArray();
  }

  private static void addCommercialAdjustments(Document document, InvoiceSummary summary, List<InvoiceLine> lines, InvoiceTotals totals, Font normal, Font bold) {
    try {
      double lineGross = lines.stream().mapToDouble(l -> (l.quantity() == null ? 1.0 : l.quantity()) * (l.price() == null ? 0.0 : l.price())).sum();
      boolean hasRows = false;
      PdfPTable adj = new PdfPTable(new float[]{8f, 2f});
      adj.setWidthPercentage(100);
      if (summary.discountPercent() != null && summary.discountPercent() > 0 && lineGross > 0) {
        adj.addCell(borderless(new Phrase("Rabatt " + summary.discountPercent() + "%", normal)));
        adj.addCell(borderless(new Phrase(EUR.format(-(lineGross * summary.discountPercent() / 100.0)), bold)));
        hasRows = true;
      }
      if (summary.couponAmount() != null && summary.couponAmount() > 0) {
        adj.addCell(borderless(new Phrase(summary.discountRemark() == null || summary.discountRemark().isBlank() ? "Gutschein" : summary.discountRemark(), normal)));
        adj.addCell(borderless(new Phrase(EUR.format(-summary.couponAmount()), bold)));
        hasRows = true;
      }
      if (hasRows) { document.add(adj); document.add(new Paragraph(" ")); }
      if (summary.installments() != null && summary.installments() > 1) {
        document.add(new Paragraph("Ratenzahlung: " + summary.installments() + " Raten à ca. " + EUR.format(totals.gross() / summary.installments()), normal));
        document.add(new Paragraph(" "));
      }
    } catch (Exception ignored) { }
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
  private String documentTitle(String number) {
    if (number == null) return "Rechnung";
    String n = number.trim();
    if (n.endsWith("S")) return "Stornorechnung";
    if (n.endsWith("G")) return "Gutschrift";
    if (n.endsWith("P")) return "Proforma-Rechnung";
    if (n.matches(".*Z\\d*$")) return "Zahlungsavis";
    return "Rechnung";
  }

}
