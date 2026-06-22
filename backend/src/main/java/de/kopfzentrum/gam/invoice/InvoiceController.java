package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import de.kopfzentrum.gam.auth.GamPermissionService;
import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import de.kopfzentrum.gam.invoice.lbd.LbdService;
import de.kopfzentrum.gam.invoice.zugferd.ZugferdExportResult;
import de.kopfzentrum.gam.invoice.zugferd.ZugferdExportService;
import de.kopfzentrum.gam.invoice.zugferd.ZugferdStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
  private final InvoiceRepository repo;
  private final InvoiceOpenHtmlPdfService openHtmlPdfService;
  private final LbdService lbdService;
  private final ZugferdExportService zugferdService;
  private final InvoiceTextPreviewService textPreviewService;
  private final GamPermissionService permissions;
  private final InvoiceAccessTokenRepository accessTokens;
  private final QrCodeService qrCodeService;
  private final String portalBaseUrl;

  public InvoiceController(InvoiceRepository repo, InvoiceOpenHtmlPdfService openHtmlPdfService, LbdService lbdService, ZugferdExportService zugferdService, InvoiceTextPreviewService textPreviewService, GamPermissionService permissions, InvoiceAccessTokenRepository accessTokens, QrCodeService qrCodeService, @Value("${app.invoice.portal.public-base-url:http://localhost:8080/api/invoice-portal}") String portalBaseUrl) {
    this.repo = repo; this.openHtmlPdfService = openHtmlPdfService; this.lbdService = lbdService; this.zugferdService = zugferdService; this.textPreviewService = textPreviewService; this.permissions = permissions; this.accessTokens = accessTokens; this.qrCodeService = qrCodeService; this.portalBaseUrl = portalBaseUrl;
  }

  @GetMapping
  public List<InvoiceSummary> search(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(required = false) Integer companyId,
    @RequestParam(required = false) Boolean creditNote,
    @RequestParam(required = false) Boolean cancelled,
    @RequestParam(required = false) Boolean paymentAdvice,
    @RequestParam(defaultValue = "100") int limit,
    @RequestParam(defaultValue = "0") int offset,
    @AuthenticationPrincipal AuthenticatedUser user
  ) {
    requireInvoiceRead(user, companyId);
    return repo.search(new InvoiceSearchCriteria(q, companyId, null, null, creditNote, cancelled, paymentAdvice, limit, offset));
  }

  @GetMapping("/numbers/next")
  public InvoiceNumberPreview nextNumber(@RequestParam(required = false) Integer companyId, @AuthenticationPrincipal AuthenticatedUser user) { requireInvoiceRead(user, companyId); return repo.nextInvoiceNumberPreview(companyId); }

  @GetMapping("/{number}")
  public InvoiceDetail detail(@PathVariable String number, @RequestParam(required = false) Integer companyId, @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceDetail d = repo.findDetail(number, companyId);
    requireInvoiceRead(user, d.summary().companyId());
    return d;
  }

  /** Pflicht-Export: OpenHTMLtoPDF + PDF/A-3 + ZUGFeRD/Factur-X + PDF/UA/WCAG. */
  @GetMapping("/{number}/pdf")
  public ResponseEntity<byte[]> pdf(@PathVariable String number, @RequestParam(defaultValue = "de") String lang, @RequestParam(required = false) Integer companyId) {
    InvoiceSummary summary = repo.findSummary(number, companyId);
    ZugferdExportResult result = zugferdService.export(number, summary.companyId(), lang);
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + result.filename())
      .header("X-GAM-PDF-Engine", "OpenHTMLtoPDF-PDFUA-PDFA3-ZUGFeRD")
      .header("X-GAM-E-Invoice", "ZUGFeRD/Factur-X")
      .header("X-GAM-ZUGFeRD-Profile", result.profile())
      .header("X-GAM-ZUGFeRD-Valid", Boolean.toString(result.valid()))
      .contentType(MediaType.APPLICATION_PDF)
      .body(result.pdfBytes());
  }

  /**
   * Kompatibilitaetsalias fuer den bisherigen Button-2-Testpfad.
   * Seit Schritt 37i identisch mit dem produktiven OpenHTMLtoPDF-Pflicht-Export.
   */
  @GetMapping("/{number}/pdf-openhtml")
  public ResponseEntity<byte[]> pdfOpenHtml(@PathVariable String number, @RequestParam(defaultValue = "de") String lang, @RequestParam(required = false) Integer companyId) {
    InvoiceSummary summary = repo.findSummary(number, companyId);
    byte[] basePdf = openHtmlPdfService.renderVisualPdf(number, summary.companyId(), lang);
    ZugferdExportResult result = zugferdService.exportWithBasePdf(number, summary.companyId(), lang, basePdf, "openhtmltopdf-zugferd");
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + result.filename())
      .header("X-GAM-PDF-Engine", "OpenHTMLtoPDF-PDFUA-PDFA3-ZUGFeRD")
      .header("X-GAM-E-Invoice", "ZUGFeRD/Factur-X")
      .header("X-GAM-ZUGFeRD-Profile", result.profile())
      .header("X-GAM-ZUGFeRD-Valid", Boolean.toString(result.valid()))
      .contentType(MediaType.APPLICATION_PDF)
      .body(result.pdfBytes());
  }

  @GetMapping("/{number}/zugferd.xml")
  public ResponseEntity<byte[]> zugferdXml(@PathVariable String number, @RequestParam(required = false) Integer companyId) {
    InvoiceSummary summary = repo.findSummary(number, companyId);
    byte[] bytes = zugferdService.xml(number, summary.companyId());
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rechnung-" + number + "-zugferd.xml")
      .contentType(MediaType.APPLICATION_XML)
      .body(bytes);
  }


  @GetMapping("/{number}/access")
  public java.util.Map<String,Object> invoiceAccess(@PathVariable String number, @RequestParam(required = false) Integer companyId, @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = repo.findSummary(number, companyId);
    requireInvoiceRead(user, summary.companyId());
    InvoiceAccessToken access = accessTokens.getOrCreate(number, summary.companyId());
    String url = portalBaseUrl.replaceAll("/$", "") + "/" + access.token();
    return java.util.Map.of("token", access.token(), "url", url, "invoiceNumber", number, "companyId", summary.companyId());
  }

  @GetMapping("/{number}/access-qr")
  public ResponseEntity<byte[]> invoiceAccessQr(@PathVariable String number, @RequestParam(required = false) Integer companyId, @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = repo.findSummary(number, companyId);
    requireInvoiceRead(user, summary.companyId());
    InvoiceAccessToken access = accessTokens.getOrCreate(number, summary.companyId());
    String url = portalBaseUrl.replaceAll("/$", "") + "/" + access.token();
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrCodeService.png(url, 240));
  }

  @GetMapping("/{number}/export-check")
  public InvoiceExportCheck exportCheck(@PathVariable String number, @RequestParam(required = false) Integer companyId, @AuthenticationPrincipal AuthenticatedUser user) { InvoiceSummary s = repo.findSummary(number, companyId); requireInvoiceReport(user, s.companyId()); return zugferdService.check(number, s.companyId()); }

  @GetMapping("/zugferd/status")
  public ZugferdStatus zugferdStatus() { return zugferdService.status(); }


  @GetMapping("/text-preview")
  public InvoiceTextPreview textPreview(
    @RequestParam(required = false) Integer companyId,
    @RequestParam(defaultValue = "de") String lang,
    @RequestParam(defaultValue = "") String treatmentDate,
    @RequestParam(defaultValue = "") String lbdFile,
    @AuthenticationPrincipal AuthenticatedUser user
  ) throws Exception {
    requireInvoiceRead(user, companyId);
    LbdRecipient lbd = lbdService.preview(lbdFile);
    return textPreviewService.preview(companyId, lang, treatmentDate, lbd);
  }

  @GetMapping("/products")
  public List<ProductDto> products(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "100") int limit) { return repo.findProducts(q, Math.min(Math.max(limit, 1), 500)); }

  @GetMapping("/companies")
  public List<InvoiceCompany> companies() { return repo.findCompanies(); }

  @GetMapping("/draft")
  public InvoiceDraft draft(@RequestParam(defaultValue = "") String lbdFile, @RequestParam(required = false) Integer companyId, @AuthenticationPrincipal AuthenticatedUser user) throws Exception {
    requireInvoiceRead(user, companyId);
    LbdRecipient lbd = lbdService.preview(lbdFile);
    return new InvoiceDraft(repo.nextInvoiceNumber(companyId), LocalDate.now().toString(), repo.findCompanies(), lbd, repo.calculate(List.of()));
  }

  @PostMapping("/calculate")
  public InvoiceTotals calculate(@RequestBody List<InvoiceCreateLineRequest> lines) { return repo.calculate(lines); }


  @PutMapping("/{number}")
  public InvoiceCreateResponse update(@PathVariable String number, @RequestBody InvoiceUpdateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    requireInvoiceWrite(user, repo.findSummary(number).companyId());
    String username = user == null ? "gam2" : user.getUsername();
    return repo.updateInvoice(number, request, username);
  }

  @PatchMapping("/{number}/status")
  public InvoiceDetail status(@PathVariable String number, @RequestBody InvoiceStatusUpdateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    requireInvoiceWrite(user, repo.findSummary(number).companyId());
    String username = user == null ? "gam2" : user.getUsername();
    return repo.updateStatus(number, request, username);
  }

  @DeleteMapping("/{number}/draft")
  public ResponseEntity<Void> deleteDraft(@PathVariable String number) {
    repo.deleteDraftCompletely(number);
    return ResponseEntity.noContent().build();
  }


  @PostMapping("/{number}/cancel")
  public InvoiceCreateResponse cancel(@PathVariable String number, @RequestParam(required = false) Integer companyId, @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = repo.findSummary(number, companyId);
    requireInvoiceWrite(user, summary.companyId());
    String username = user == null ? "gam2" : user.getUsername();
    return repo.createCancellationInvoice(number, summary.companyId(), username);
  }

  @PostMapping("/{number}/credit")
  public InvoiceCreateResponse credit(@PathVariable String number, @RequestParam(required = false) Integer companyId, @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = repo.findSummary(number, companyId);
    requireInvoiceWrite(user, summary.companyId());
    String username = user == null ? "gam2" : user.getUsername();
    return repo.createCreditNote(number, summary.companyId(), username);
  }

  @PostMapping("/proforma")
  public InvoiceCreateResponse proforma(@RequestBody InvoiceCreateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    requireInvoiceWrite(user, request.companyId());
    String username = user == null ? "gam2" : user.getUsername();
    return repo.createProformaInvoice(request, username);
  }

  @PostMapping
  public InvoiceCreateResponse create(@RequestBody InvoiceCreateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    requireInvoiceWrite(user, request.companyId());
    String username = user == null ? "gam2" : user.getUsername();
    return repo.createInvoice(request, username);
  }


  private void requireInvoiceRead(AuthenticatedUser user, Integer companyId) {
    if (user == null || !permissions.canOpen(user.account(), "Rechnungsprogramm", companyId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kein Zugriff auf Rechnungsprogramm/Gesellschaft");
    }
  }

  private void requireInvoiceReport(AuthenticatedUser user, Integer companyId) {
    if (user == null || !permissions.canReport(user.account(), "Rechnungsprogramm", companyId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Report-/Exportberechtigung fuer diese Gesellschaft");
    }
  }

  private void requireInvoiceWrite(AuthenticatedUser user, Integer companyId) {
    if (user == null || !permissions.canWrite(user.account(), "Rechnungsprogramm", companyId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Schreibberechtigung fuer Rechnungsprogramm/Gesellschaft");
    }
  }

}
