package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
  private final InvoiceRepository repo;
  private final InvoicePdfService pdfService;
  private final LbdService lbdService;
  private final ZugferdExportService zugferdService;

  public InvoiceController(InvoiceRepository repo, InvoicePdfService pdfService, LbdService lbdService, ZugferdExportService zugferdService) {
    this.repo = repo; this.pdfService = pdfService; this.lbdService = lbdService; this.zugferdService = zugferdService;
  }

  @GetMapping
  public List<InvoiceSummary> search(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(required = false) Integer companyId,
    @RequestParam(required = false) Boolean creditNote,
    @RequestParam(required = false) Boolean cancelled,
    @RequestParam(required = false) Boolean paymentAdvice,
    @RequestParam(defaultValue = "100") int limit,
    @RequestParam(defaultValue = "0") int offset
  ) {
    return repo.search(new InvoiceSearchCriteria(q, companyId, null, null, creditNote, cancelled, paymentAdvice, limit, offset));
  }

  @GetMapping("/numbers/next")
  public InvoiceNumberPreview nextNumber() { return repo.nextInvoiceNumberPreview(); }

  @GetMapping("/{number}")
  public InvoiceDetail detail(@PathVariable String number) { return repo.findDetail(number); }

  /** Pflicht-Export: sichtbares PDF + eingebettete ZUGFeRD/Factur-X XML. */
  @GetMapping("/{number}/pdf")
  public ResponseEntity<byte[]> pdf(@PathVariable String number) {
    ZugferdExportResult result = zugferdService.export(number);
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + result.filename())
      .header("X-GAM-E-Invoice", "ZUGFeRD/Factur-X")
      .header("X-GAM-ZUGFeRD-Profile", result.profile())
      .header("X-GAM-ZUGFeRD-Valid", Boolean.toString(result.valid()))
      .contentType(MediaType.APPLICATION_PDF)
      .body(result.pdfBytes());
  }

  @GetMapping("/{number}/pdf-debug")
  public ResponseEntity<byte[]> pdfDebug(@PathVariable String number) {
    byte[] bytes = pdfService.render(number);
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=rechnung-" + number + "-debug.pdf")
      .contentType(MediaType.APPLICATION_PDF)
      .body(bytes);
  }

  @GetMapping("/{number}/zugferd.xml")
  public ResponseEntity<byte[]> zugferdXml(@PathVariable String number) {
    byte[] bytes = zugferdService.xml(number);
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rechnung-" + number + "-zugferd.xml")
      .contentType(MediaType.APPLICATION_XML)
      .body(bytes);
  }

  @GetMapping("/{number}/export-check")
  public InvoiceExportCheck exportCheck(@PathVariable String number) { return zugferdService.check(number); }

  @GetMapping("/zugferd/status")
  public ZugferdStatus zugferdStatus() { return zugferdService.status(); }

  @GetMapping("/products")
  public List<ProductDto> products(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "100") int limit) { return repo.findProducts(q, Math.min(Math.max(limit, 1), 500)); }

  @GetMapping("/companies")
  public List<InvoiceCompany> companies() { return repo.findCompanies(); }

  @GetMapping("/draft")
  public InvoiceDraft draft(@RequestParam(defaultValue = "") String lbdFile) throws Exception {
    LbdRecipient lbd = lbdService.preview(lbdFile);
    return new InvoiceDraft(repo.nextInvoiceNumber(), LocalDate.now().toString(), repo.findCompanies(), lbd, repo.calculate(List.of()));
  }

  @PostMapping("/calculate")
  public InvoiceTotals calculate(@RequestBody List<InvoiceCreateLineRequest> lines) { return repo.calculate(lines); }


  @PutMapping("/{number}")
  public InvoiceCreateResponse update(@PathVariable String number, @RequestBody InvoiceUpdateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    String username = user == null ? "gam2" : user.getUsername();
    return repo.updateInvoice(number, request, username);
  }

  @PatchMapping("/{number}/status")
  public InvoiceDetail status(@PathVariable String number, @RequestBody InvoiceStatusUpdateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    String username = user == null ? "gam2" : user.getUsername();
    return repo.updateStatus(number, request, username);
  }

  @DeleteMapping("/{number}/draft")
  public ResponseEntity<Void> deleteDraft(@PathVariable String number) {
    repo.deleteDraftCompletely(number);
    return ResponseEntity.noContent().build();
  }


  @PostMapping("/{number}/cancel")
  public InvoiceCreateResponse cancel(@PathVariable String number, @AuthenticationPrincipal AuthenticatedUser user) {
    String username = user == null ? "gam2" : user.getUsername();
    return repo.createCancellationInvoice(number, username);
  }

  @PostMapping("/{number}/credit")
  public InvoiceCreateResponse credit(@PathVariable String number, @AuthenticationPrincipal AuthenticatedUser user) {
    String username = user == null ? "gam2" : user.getUsername();
    return repo.createCreditNote(number, username);
  }

  @PostMapping("/proforma")
  public InvoiceCreateResponse proforma(@RequestBody InvoiceCreateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    String username = user == null ? "gam2" : user.getUsername();
    return repo.createProformaInvoice(request, username);
  }

  @PostMapping
  public InvoiceCreateResponse create(@RequestBody InvoiceCreateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    String username = user == null ? "gam2" : user.getUsername();
    return repo.createInvoice(request, username);
  }
}
