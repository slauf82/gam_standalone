package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import de.kopfzentrum.gam.auth.GamPermissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices/{number}/workflow")
public class InvoiceWorkflowController {
  private final InvoiceRepository invoices;
  private final InvoiceWorkflowRepository workflow;
  private final GamPermissionService permissions;

  public InvoiceWorkflowController(InvoiceRepository invoices, InvoiceWorkflowRepository workflow, GamPermissionService permissions) {
    this.invoices = invoices; this.workflow = workflow; this.permissions = permissions;
  }

  @GetMapping
  public InvoiceWorkflowState state(@PathVariable String number, @RequestParam(required = false) Integer companyId,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = invoices.findSummary(number, companyId);
    requireRead(user, summary.companyId());
    return workflow.getOrCreate(number, summary.companyId(), username(user));
  }

  @PostMapping("/transition")
  public InvoiceWorkflowState transition(@PathVariable String number, @RequestParam(required = false) Integer companyId,
                                         @RequestBody InvoiceWorkflowUpdateRequest request,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
    InvoiceSummary summary = invoices.findSummary(number, companyId);
    requireWrite(user, summary.companyId());
    return workflow.transition(number, summary.companyId(), request.status(), request.note(), username(user));
  }

  private String username(AuthenticatedUser user) { return user == null ? "gam2" : user.getUsername(); }
  private void requireRead(AuthenticatedUser user, Integer companyId) {
    if (user == null || !permissions.canOpen(user.account(), "Rechnungsprogramm", companyId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
  }
  private void requireWrite(AuthenticatedUser user, Integer companyId) {
    if (user == null || !permissions.canWrite(user.account(), "Rechnungsprogramm", companyId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
  }
}
