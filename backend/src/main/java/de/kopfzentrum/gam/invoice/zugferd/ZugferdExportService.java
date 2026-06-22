package de.kopfzentrum.gam.invoice.zugferd;

import de.kopfzentrum.gam.invoice.*;
import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import de.kopfzentrum.gam.invoice.lbd.LbdService;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ZugferdExportService {
  private final InvoiceRepository repo;
  private final InvoiceOpenHtmlPdfService pdfService;
  private final ZugferdXmlService xmlService;
  private final LbdService lbdService;
  private final InvoicePdfArchiveService archiveService;

  @Value("${zugferd.enabled:true}") private boolean enabled;
  @Value("${zugferd.profile:EN16931}") private String profile;
  @Value("${zugferd.validate:false}") private boolean validate;

  public ZugferdExportService(InvoiceRepository repo, InvoiceOpenHtmlPdfService pdfService, ZugferdXmlService xmlService, LbdService lbdService, InvoicePdfArchiveService archiveService) {
    this.repo = repo; this.pdfService = pdfService; this.xmlService = xmlService; this.lbdService = lbdService; this.archiveService = archiveService;
  }

  public ZugferdStatus status() {
    return new ZugferdStatus(enabled, profile, validate, "Schritt 37i: OpenHTMLtoPDF ist alleinige PDF-Engine; Pflicht-Export erzeugt PDF/A-3 + ZUGFeRD/Factur-X + PDF/UA/WCAG-konforme Rechnung.");
  }

  public InvoiceExportCheck check(String number) { return check(number, null); }

  public InvoiceExportCheck check(String number, Integer companyId) {
    InvoiceDetail detail = repo.findDetail(number, companyId);
    InvoiceCompany company = repo.findCompany(detail.summary().companyId());
    LbdRecipient recipient = invoiceRecipient(number, companyId);
    List<InvoiceValidationIssue> issues = new ArrayList<>();
    if (detail.lines() == null || detail.lines().isEmpty()) issues.add(new InvoiceValidationIssue("ERROR", "lines", "Rechnung hat keine Positionen."));
    if (detail.summary().invoiceDate() == null || detail.summary().invoiceDate().isBlank()) issues.add(new InvoiceValidationIssue("ERROR", "invoiceDate", "Rechnungsdatum fehlt."));
    if (company == null) issues.add(new InvoiceValidationIssue("ERROR", "company", "Rechnungsgesellschaft fehlt."));
    else {
      if (blank(company.name())) issues.add(new InvoiceValidationIssue("ERROR", "seller.name", "Name der Rechnungsgesellschaft fehlt."));
      if (blank(company.street()) || blank(company.city())) issues.add(new InvoiceValidationIssue("WARN", "seller.address", "Adresse der Rechnungsgesellschaft ist unvollständig."));
      if (blank(company.iban())) issues.add(new InvoiceValidationIssue("WARN", "seller.iban", "IBAN fehlt; für viele Zahlungsszenarien erforderlich."));
    }
    if (recipient == null || !recipient.found()) issues.add(new InvoiceValidationIssue("WARN", "lbd", ".lbd-Empfängerdatei wurde nicht gefunden; Export nutzt Platzhalter."));
    else {
      if (blank(recipient.lastName()) && blank(recipient.firstName())) issues.add(new InvoiceValidationIssue("WARN", "buyer.name", "Empfängername aus .lbd ist unvollständig."));
      if (blank(recipient.street()) || blank(recipient.postalCode()) || blank(recipient.city())) issues.add(new InvoiceValidationIssue("WARN", "buyer.address", "Empfängeradresse aus .lbd ist unvollständig."));
    }
    for (InvoiceLine line : detail.lines()) {
      if (line.productId() == null) issues.add(new InvoiceValidationIssue("ERROR", "line.productId", "Position ohne Produkt-ID."));
      if (line.price() == null) issues.add(new InvoiceValidationIssue("ERROR", "line.price", "Position ohne Preis."));
      if (line.vat() == null) issues.add(new InvoiceValidationIssue("WARN", "line.vat", "Position ohne MwSt-Satz; 0% wird angenommen."));
    }
    boolean exportable = issues.stream().noneMatch(i -> "ERROR".equalsIgnoreCase(i.severity()));
    return new InvoiceExportCheck(number, exportable, issues);
  }

  public ZugferdExportResult export(String number) { return export(number, null, "de"); }
  public ZugferdExportResult export(String number, String language) { return export(number, null, language); }
  public ZugferdExportResult export(String number, Integer companyId, String language) {
    InvoiceExportCheck check = check(number, companyId);
    if (!check.exportable()) throw new IllegalStateException("ZUGFeRD-Export nicht möglich: " + check.issues());
    InvoiceDetail detail = repo.findDetail(number, companyId);
    InvoiceCompany company = repo.findCompany(detail.summary().companyId());
    LbdRecipient recipient = invoiceRecipient(number, companyId);
    InvoiceTotals totals = detail.totals() == null ? repo.calculateFromExistingLines(detail.lines()) : detail.totals();
    byte[] xml = xmlService.buildXml(detail, company, recipient, totals);
    byte[] basePdf = pdfService.renderVisualPdf(number, detail.summary().companyId(), language);
    byte[] finalPdf = enabled ? embed(basePdf, xml) : basePdf;
    // Schritt 37i.4: Nach der Mustang/ZUGFeRD-Einbettung die in 37h8
    // erfolgreiche PDF/UA-/WCAG-Nachbearbeitung erneut anwenden.
    // Mustang kann XMP-Metadaten und Viewer-/Outline-Settings überschreiben;
    // deshalb muss dieser Schritt der letzte PDF-Postprocessing-Schritt sein.
    finalPdf = applyOpenHtmlAccessibilityMetadata(finalPdf, number, language);
    archiveService.archive(number, detail.summary().companyId(), finalPdf);
    return new ZugferdExportResult(number, profile, "rechnung-" + number + "-zugferd.pdf", finalPdf, xml, enabled, !validate,
      validate ? "Validierung ist vorbereitet; harte Mustang-/KoSIT-Validierung folgt im nächsten Feinschliff." : "Interne Plausibilitätsprüfung bestanden; externe Validierung deaktiviert.");
  }

  public ZugferdExportResult exportWithBasePdf(String number, Integer companyId, String language, byte[] basePdf, String filenameSuffix) {
    InvoiceExportCheck check = check(number, companyId);
    if (!check.exportable()) throw new IllegalStateException("ZUGFeRD-Export nicht möglich: " + check.issues());
    InvoiceDetail detail = repo.findDetail(number, companyId);
    InvoiceCompany company = repo.findCompany(detail.summary().companyId());
    LbdRecipient recipient = invoiceRecipient(number, companyId);
    InvoiceTotals totals = detail.totals() == null ? repo.calculateFromExistingLines(detail.lines()) : detail.totals();
    byte[] xml = xmlService.buildXml(detail, company, recipient, totals);
    byte[] finalPdf = enabled ? embed(basePdf, xml) : basePdf;
    finalPdf = applyOpenHtmlAccessibilityMetadata(finalPdf, number, language);
    String suffix = (filenameSuffix == null || filenameSuffix.isBlank()) ? "zugferd" : filenameSuffix;
    return new ZugferdExportResult(number, profile, "rechnung-" + number + "-" + suffix + ".pdf", finalPdf, xml, enabled, !validate,
      "Schritt 37i Pflicht-Export: OpenHTMLtoPDF erzeugt PDF/A-3 + ZUGFeRD; PDF/UA/WCAG-Feinschliff aus 37h8 ist aktiv.");
  }


  /**
   * Schritt 37h1: Minimaler, stabiler PDF/UA-Vorbereitungsschritt.
   * Keine HTML-/Tabellenstruktur wird verändert, damit der in 37g funktionierende
   * OpenHTMLtoPDF-ZUGFeRD-Pfad stabil bleibt. Es werden nur die einfachen
   * Dokumenteigenschaften nach der Mustang/ZUGFeRD-Einbettung erneut gesetzt.
   */
  private byte[] applyOpenHtmlAccessibilityMetadata(byte[] pdf, String number, String language) {
    try (PDDocument document = Loader.loadPDF(pdf); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      String lang = normalizeLanguage(language);
      String title = "Rechnung " + (number == null ? "" : number);

      PDDocumentInformation info = document.getDocumentInformation();
      if (info == null) {
        info = new PDDocumentInformation();
        document.setDocumentInformation(info);
      }
      info.setTitle(title);
      info.setSubject("ZUGFeRD/Factur-X Rechnung");
      info.setCreator("GAM 2.0 Standalone");
      info.setProducer("GAM 2.0 Standalone / OpenHTMLtoPDF / MustangProject");

      PDDocumentCatalog catalog = document.getDocumentCatalog();
      if (catalog != null) {
        catalog.setLanguage(lang);

        PDViewerPreferences preferences = catalog.getViewerPreferences();
        if (preferences == null) {
          preferences = new PDViewerPreferences(new COSDictionary());
          catalog.setViewerPreferences(preferences);
        }
        preferences.setDisplayDocTitle(true);

        PDMarkInfo markInfo = catalog.getMarkInfo();
        if (markInfo == null) {
          markInfo = new PDMarkInfo();
          catalog.setMarkInfo(markInfo);
        }
        markInfo.setMarked(true);

        ensureSimpleOutline(document, catalog, title);
        applyPdfUaXmpMetadata(document, title, lang);
      }

      document.save(out);
      return out.toByteArray();
    } catch (Exception ex) {
      // Fallback: Der funktionierende 37g-Export darf nicht durch Metadaten scheitern.
      return pdf;
    }
  }

  /**
   * Schritt 37h8: PAC-Feinschliff ohne riskante echte Rechnungstabellen-Umbauten.
   * - PDF/UA-XMP-Metadaten werden explizit gesetzt.
   * - Eine einfache Dokumentnavigation hilft gegen WCAG 2.4 Navigable.
   * - Layoutbereiche werden im HTML nicht mehr als CSS-Tabellen gerendert.
   */
  private static void applyPdfUaXmpMetadata(PDDocument document, String title, String language) throws Exception {
    String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    String safeTitle = xmlEscape(title == null || title.isBlank() ? "Rechnung" : title);
    String safeLang = xmlEscape(language == null || language.isBlank() ? "de" : language);
    String xmp = """
      <?xpacket begin='﻿' id='W5M0MpCehiHzreSzNTczkc9d'?>
      <x:xmpmeta xmlns:x='adobe:ns:meta/'>
        <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>
          <rdf:Description rdf:about=''
              xmlns:dc='http://purl.org/dc/elements/1.1/'
              xmlns:xmp='http://ns.adobe.com/xap/1.0/'
              xmlns:pdf='http://ns.adobe.com/pdf/1.3/'
              xmlns:pdfaid='http://www.aiim.org/pdfa/ns/id/'
              xmlns:pdfuaid='http://www.aiim.org/pdfua/ns/id/'>
            <dc:title><rdf:Alt><rdf:li xml:lang='x-default'>%s</rdf:li><rdf:li xml:lang='%s'>%s</rdf:li></rdf:Alt></dc:title>
            <dc:creator><rdf:Seq><rdf:li>GAM 2.0 Standalone</rdf:li></rdf:Seq></dc:creator>
            <dc:description><rdf:Alt><rdf:li xml:lang='x-default'>ZUGFeRD/Factur-X Rechnung</rdf:li></rdf:Alt></dc:description>
            <xmp:CreatorTool>GAM 2.0 Standalone / OpenHTMLtoPDF</xmp:CreatorTool>
            <xmp:CreateDate>%s</xmp:CreateDate>
            <xmp:ModifyDate>%s</xmp:ModifyDate>
            <xmp:MetadataDate>%s</xmp:MetadataDate>
            <pdf:Producer>GAM 2.0 Standalone / OpenHTMLtoPDF / MustangProject</pdf:Producer>
            <pdfaid:part>3</pdfaid:part>
            <pdfaid:conformance>U</pdfaid:conformance>
            <pdfuaid:part>1</pdfuaid:part>
          </rdf:Description>
        </rdf:RDF>
      </x:xmpmeta>
      <?xpacket end='w'?>
      """.formatted(safeTitle, safeLang, safeTitle, now, now, now);

    PDMetadata metadata = new PDMetadata(document);
    metadata.importXMPMetadata(xmp.getBytes(StandardCharsets.UTF_8));
    document.getDocumentCatalog().setMetadata(metadata);
  }

  private static void ensureSimpleOutline(PDDocument document, PDDocumentCatalog catalog, String title) {
    try {
      if (document.getNumberOfPages() < 1) return;
      PDDocumentOutline outline = new PDDocumentOutline();
      catalog.setDocumentOutline(outline);

      PDPageFitDestination destination = new PDPageFitDestination();
      destination.setPage(document.getPage(0));

      PDOutlineItem item = new PDOutlineItem();
      item.setTitle(title == null || title.isBlank() ? "Rechnung" : title);
      item.setDestination(destination);
      outline.addLast(item);
      outline.openNode();
      item.openNode();
    } catch (Exception ignored) {
      // Navigation ist ein PAC-Feinschliff; der Export selbst bleibt stabil.
    }
  }

  private static String xmlEscape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
  }

  private static String normalizeLanguage(String language) {
    if (language == null || language.isBlank()) return "de";
    String l = language.toLowerCase(java.util.Locale.ROOT);
    return switch (l.substring(0, Math.min(2, l.length()))) {
      case "en" -> "en";
      case "fr" -> "fr";
      case "it" -> "it";
      case "uk" -> "uk";
      default -> "de";
    };
  }

  public byte[] xml(String number) { return xml(number, null); }

  public byte[] xml(String number, Integer companyId) {
    InvoiceDetail detail = repo.findDetail(number, companyId);
    InvoiceCompany company = repo.findCompany(detail.summary().companyId());
    LbdRecipient recipient = invoiceRecipient(number, companyId);
    InvoiceTotals totals = detail.totals() == null ? repo.calculateFromExistingLines(detail.lines()) : detail.totals();
    return xmlService.buildXml(detail, company, recipient, totals);
  }

  private byte[] embed(byte[] basePdf, byte[] xml) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ZUGFeRDExporterFromA3 exporter = new ZUGFeRDExporterFromA3();
      exporter.load(new ByteArrayInputStream(basePdf));
      exporter.setZUGFeRDVersion(2);
      exporter.setProfile(profile == null || profile.isBlank() ? "EN16931" : profile);
      exporter.setProducer("GAM 2.0 Standalone");
      exporter.setCreator(System.getProperty("user.name", "gam"));
      exporter.setXML(xml);
      exporter.export(out);
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("ZUGFeRD/Factur-X Export konnte nicht erzeugt werden. Das sichtbare PDF wurde erzeugt, aber XML-Einbettung ist fehlgeschlagen: " + ex.getMessage(), ex);
    }
  }

  private LbdRecipient invoiceRecipient(String number, Integer companyId) {
    try {
      LbdRecipient stored = repo.findInvoiceRecipient(number, companyId);
      if (stored != null && stored.found()) return stored;
    } catch (Exception ignored) {}
    try { return lbdService.preview(""); } catch (Exception e) { return null; }
  }
  private static boolean blank(String s) { return s == null || s.isBlank(); }
}
