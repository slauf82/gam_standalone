package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings/invoice-workflow")
public class InvoiceWorkflowSettingsController {
  private final InvoiceWorkflowSettingsRepository settings;

  public InvoiceWorkflowSettingsController(InvoiceWorkflowSettingsRepository settings) { this.settings = settings; }

  @GetMapping
  public InvoiceWorkflowSettings get(@AuthenticationPrincipal AuthenticatedUser user) {
    requireUser(user);
    return settings.load();
  }

  @PutMapping
  public InvoiceWorkflowSettings save(@RequestBody InvoiceWorkflowSettings request,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
    requireUser(user);
    return settings.save(request, user.getUsername());
  }

  private void requireUser(AuthenticatedUser user) {
    if (user == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }
}
