package de.kopfzentrum.gam.invoice.zugferd;

import de.kopfzentrum.gam.invoice.*;
import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import de.kopfzentrum.gam.invoice.lbd.LbdService;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ZugferdExportService {
  private final InvoiceRepository repo;
  private final InvoicePdfService pdfService;
  private final ZugferdXmlService xmlService;
  private final LbdService lbdService;
  private final InvoicePdfArchiveService archiveService;

  @Value("${zugferd.enabled:true}") private boolean enabled;
  @Value("${zugferd.profile:EN16931}") private String profile;
  @Value("${zugferd.validate:false}") private boolean validate;

  public ZugferdExportService(InvoiceRepository repo, InvoicePdfService pdfService, ZugferdXmlService xmlService, LbdService lbdService, InvoicePdfArchiveService archiveService) {
    this.repo = repo; this.pdfService = pdfService; this.xmlService = xmlService; this.lbdService = lbdService; this.archiveService = archiveService;
  }

  public ZugferdStatus status() {
    return new ZugferdStatus(enabled, profile, validate, "Schritt 1: ZUGFeRD/Factur-X ist Pflicht-Export; Debug-PDF bleibt nur Fallback.");
  }

  public InvoiceExportCheck check(String number) {
    InvoiceDetail detail = repo.findDetail(number);
    InvoiceCompany company = repo.findCompany(detail.summary().companyId());
    LbdRecipient recipient = previewRecipient();
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

  public ZugferdExportResult export(String number) { return export(number, "de"); }

  public ZugferdExportResult export(String number, String language) {
    InvoiceExportCheck check = check(number);
    if (!check.exportable()) throw new IllegalStateException("ZUGFeRD-Export nicht möglich: " + check.issues());
    InvoiceDetail detail = repo.findDetail(number);
    InvoiceCompany company = repo.findCompany(detail.summary().companyId());
    LbdRecipient recipient = previewRecipient();
    InvoiceTotals totals = detail.totals() == null ? repo.calculateFromExistingLines(detail.lines()) : detail.totals();
    byte[] xml = xmlService.buildXml(detail, company, recipient, totals);
    byte[] basePdf = pdfService.renderVisualPdf(number, true, language);
    byte[] finalPdf = enabled ? embed(basePdf, xml) : basePdf;
    archiveService.archive(number, finalPdf);
    return new ZugferdExportResult(number, profile, "rechnung-" + number + "-zugferd.pdf", finalPdf, xml, enabled, !validate,
      validate ? "Validierung ist vorbereitet; harte Mustang-/KoSIT-Validierung folgt im nächsten Feinschliff." : "Interne Plausibilitätsprüfung bestanden; externe Validierung deaktiviert.");
  }

  public byte[] xml(String number) {
    InvoiceDetail detail = repo.findDetail(number);
    InvoiceCompany company = repo.findCompany(detail.summary().companyId());
    LbdRecipient recipient = previewRecipient();
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

  private LbdRecipient previewRecipient() { try { return lbdService.preview(""); } catch (Exception e) { return null; } }
  private static boolean blank(String s) { return s == null || s.isBlank(); }
}
