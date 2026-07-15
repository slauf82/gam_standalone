package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import de.kopfzentrum.gam.auth.GamPermissionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/invoices/{number}/payment-workflow")
public class PaymentWorkflowController {
  private final InvoiceRepository invoices;
  private final PaymentWorkflowRepository payments;
  private final GamPermissionService permissions;
  private final PaymentDocumentService documents;

  public PaymentWorkflowController(InvoiceRepository invoices, PaymentWorkflowRepository payments,
                                   GamPermissionService permissions, PaymentDocumentService documents) {
    this.invoices = invoices;
    this.payments = payments;
    this.permissions = permissions;
    this.documents = documents;
  }

  @GetMapping
  public PaymentWorkflowState state(@PathVariable String number,
                                    @RequestParam(required = false) Integer companyId,
                                    @RequestParam(required = false) BigDecimal amountDue,
                                    @RequestParam(required = false) LocalDate dueDate,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = invoices.findSummary(number, companyId);
    requireRead(user, summary.companyId());
    return payments.getOrCreate(number, summary.companyId(), amountDue, dueDate, username(user));
  }

  @PostMapping
  public PaymentWorkflowState update(@PathVariable String number,
                                     @RequestParam(required = false) Integer companyId,
                                     @RequestBody PaymentWorkflowUpdateRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = invoices.findSummary(number, companyId);
    requireWrite(user, summary.companyId());
    PaymentWorkflowState updated = payments.update(number, summary.companyId(), request, username(user));
    String action = request == null || request.action() == null ? "" : request.action().trim().toUpperCase(java.util.Locale.ROOT);
    if (action.equals("AUFSCHUB_1_TAG") || action.equals("AUFSCHUB_3_TAGE") || action.equals("AUFSCHUB_7_TAGE")) {
      PaymentDocument currentDocument = payments.latestDocument(number, summary.companyId());
      if (currentDocument != null) {
        byte[] refreshedPdf = documents.render(number, summary.companyId(), currentDocument.documentType(),
            currentDocument.language(), updated.openAmount(), updated.dueDate());
        payments.updateDocumentPdf(currentDocument.id(), refreshedPdf);
      }
    }
    return updated;
  }


  @PostMapping("/documents/{type}")
  public PaymentDocument createDocument(@PathVariable String number,
                                        @PathVariable String type,
                                        @RequestParam(required = false) Integer companyId,
                                        @RequestParam(defaultValue = "de") String lang,
                                        @RequestParam(defaultValue = "false") boolean repeat,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = invoices.findSummary(number, companyId);
    requireWrite(user, summary.companyId());
    PaymentWorkflowState state = payments.getOrCreate(number, summary.companyId(), null, null, username(user));
    String normalized = normalizeDocumentType(type);
    if (payments.hasDocumentType(number, summary.companyId(), normalized) && !repeat) throw new ResponseStatusException(HttpStatus.CONFLICT,"Dieses Schreiben wurde bereits erzeugt. Erneute Erstellung muss bestätigt werden.");
    String title = PaymentDocumentService.localizedTitle(normalized, lang);
    byte[] pdf = documents.render(number, summary.companyId(), normalized, lang, state.openAmount(), state.dueDate());
    PaymentDocument saved = payments.saveDocument(number, summary.companyId(), normalized, title, lang, pdf, username(user));
    String action = switch (normalized) {case "REMINDER" -> "ZAHLUNGSERINNERUNG"; case "DUNNING_1" -> "MAHNUNG_1"; case "DUNNING_2" -> "MAHNUNG_2"; case "DUNNING_3" -> "MAHNUNG_3"; case "COLLECTION" -> "INKASSO"; default -> null;};
    if (action != null) payments.update(number, summary.companyId(), new PaymentWorkflowUpdateRequest(action, null, null, null, title + " erzeugt"), username(user));
    return saved;
  }

  @GetMapping("/documents")
  public java.util.List<PaymentDocument> listDocuments(@PathVariable String number,
                                                        @RequestParam(required = false) Integer companyId,
                                                        @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = invoices.findSummary(number, companyId); requireRead(user, summary.companyId());
    return payments.documents(number, summary.companyId());
  }

  @GetMapping("/documents/{id}/pdf")
  public org.springframework.http.ResponseEntity<byte[]> documentPdf(@PathVariable String number, @PathVariable long id,
      @RequestParam(required = false) Integer companyId,
      @RequestParam(required = false) String lang,
      @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = invoices.findSummary(number, companyId); requireRead(user, summary.companyId());
    PaymentDocument doc=payments.document(id);
    if(!number.equals(doc.invoiceNumber()) || !java.util.Objects.equals(summary.companyId(),doc.companyId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    PaymentWorkflowState current=payments.getOrCreate(number, summary.companyId(), null, null, username(user));
    String documentLanguage = (lang == null || lang.isBlank()) ? doc.language() : lang;
    byte[] pdf=documents.render(number, summary.companyId(), doc.documentType(), documentLanguage, current.openAmount(), current.dueDate());
    payments.updateDocumentPdf(id,pdf);
    String localizedTitle = PaymentDocumentService.localizedTitle(doc.documentType(), documentLanguage);
    return org.springframework.http.ResponseEntity.ok().header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+localizedTitle.replace(' ','_')+".pdf\"").contentType(org.springframework.http.MediaType.APPLICATION_PDF).body(pdf);
  }

  private static String normalizeDocumentType(String value) {
    return switch (value == null ? "" : value.toUpperCase(java.util.Locale.ROOT)) {
      case "REMINDER", "ZAHLUNGSERINNERUNG" -> "REMINDER"; case "DUNNING_1", "MAHNUNG_1" -> "DUNNING_1";
      case "DUNNING_2", "MAHNUNG_2" -> "DUNNING_2"; case "DUNNING_3", "MAHNUNG_3" -> "DUNNING_3";
      case "COLLECTION", "INKASSO" -> "COLLECTION"; default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unbekannte Dokumentart");
    };
  }

  private String username(AuthenticatedUser user) {
    return user == null ? "gam2" : user.getUsername();
  }

  private void requireRead(AuthenticatedUser user, Integer companyId) {
    if (user == null || !permissions.canOpen(user.account(), "Rechnungsprogramm", companyId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Kein Zugriff auf Zahlungsworkflow/Rechnungsprogramm/Gesellschaft");
    }
  }

  private void requireWrite(AuthenticatedUser user, Integer companyId) {
    if (user == null || !permissions.canWrite(user.account(), "Rechnungsprogramm", companyId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Keine Schreibberechtigung fuer Zahlungsworkflow/Rechnungsprogramm/Gesellschaft");
    }
  }
}
