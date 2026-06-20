package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import de.kopfzentrum.gam.invoice.lbd.LbdService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class InvoicePdfArchiveService {
  private final InvoiceRepository repo;
  private final LbdService lbdService;

  @Value("${app.invoice.pdf.archive-enabled:true}")
  private boolean archiveEnabled;

  @Value("${app.invoice.pdf.archive-folder:./daten/rechnung/archiv}")
  private String archiveFolder;

  @Value("${app.invoice.pdf.hotfolder-enabled:false}")
  private boolean hotfolderEnabled;

  @Value("${app.invoice.pdf.hotfolder-folder:./daten/rechnung/hotfolder}")
  private String hotfolderFolder;

  @Value("${app.invoice.pdf.hotfolder-filename-mode:patient-number}")
  private String hotfolderFilenameMode;

  public InvoicePdfArchiveService(InvoiceRepository repo, LbdService lbdService) {
    this.repo = repo;
    this.lbdService = lbdService;
  }

  public void archive(String invoiceNumber, byte[] pdfBytes) { archive(invoiceNumber, null, pdfBytes); }

  public void archive(String invoiceNumber, Integer companyId, byte[] pdfBytes) {
    if (pdfBytes == null || pdfBytes.length == 0 || invoiceNumber == null || invoiceNumber.isBlank()) return;

    InvoiceDetail detail = repo.findDetail(invoiceNumber, companyId);
    InvoiceSummary summary = detail.summary();
    InvoiceCompany company = repo.findCompany(summary.companyId());
    LbdRecipient recipient = previewRecipient();

    if (archiveEnabled) {
      writeSafe(Paths.get(archiveFolder), archiveFileName(summary, company, recipient), pdfBytes);
    }

    if (hotfolderEnabled) {
      writeSafe(Paths.get(hotfolderFolder), hotfolderFileName(summary, recipient), pdfBytes);
    }
  }

  private void writeSafe(Path folder, String fileName, byte[] pdfBytes) {
    try {
      Files.createDirectories(folder);
      Files.write(folder.resolve(fileName), pdfBytes);
    } catch (Exception ex) {
      throw new IllegalStateException("PDF konnte nicht in Ordner geschrieben werden: " + folder + " / " + fileName + " - " + ex.getMessage(), ex);
    }
  }

  private String archiveFileName(InvoiceSummary summary, InvoiceCompany company, LbdRecipient recipient) {
    String type = invoiceType(summary.number());
    String companyName = company == null ? "Gesellschaft" : firstNonBlank(company.name(), company.code(), "Gesellschaft");
    String patient = recipient == null || !recipient.found()
      ? "Empfaenger_unbekannt"
      : (firstNonBlank(recipient.lastName(), "Nachname") + "_" + firstNonBlank(recipient.firstName(), "Vorname"));
    String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.GERMANY));
    return clean(type + "_" + summary.number() + "_" + companyName + "_" + patient + "_" + date) + ".pdf";
  }

  private String hotfolderFileName(InvoiceSummary summary, LbdRecipient recipient) {
    String mode = hotfolderFilenameMode == null ? "patient-number" : hotfolderFilenameMode.trim().toLowerCase(Locale.ROOT);
    String patientNumber = recipient == null ? "" : firstNonBlank(recipient.patientNumber(), "");
    if ("patient-number".equals(mode) && !patientNumber.isBlank()) {
      return clean(patientNumber) + ".pdf";
    }
    if ("patient-number-and-invoice".equals(mode) && !patientNumber.isBlank()) {
      return clean(patientNumber + "_" + summary.number()) + ".pdf";
    }
    return clean(summary.number()) + ".pdf";
  }

  private LbdRecipient previewRecipient() {
    try { return lbdService.preview(""); } catch (Exception e) { return null; }
  }

  private static String invoiceType(String number) {
    if (number == null) return "Rechnung";
    if (number.endsWith("S")) return "Storno";
    if (number.endsWith("G")) return "Gutschrift";
    if (number.endsWith("P")) return "Proforma";
    return "Rechnung";
  }

  private static String firstNonBlank(String... values) {
    if (values == null) return "";
    for (String value : values) if (value != null && !value.isBlank()) return value.trim();
    return "";
  }

  private static String clean(String value) {
    String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD);
    return normalized
      .replaceAll("[\\p{M}]", "")
      .replaceAll("[^A-Za-z0-9._-]+", "_")
      .replaceAll("_+", "_")
      .replaceAll("^_+|_+$", "");
  }
}
