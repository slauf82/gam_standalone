package de.kopfzentrum.gam.invoice.lbd;

import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/invoices/lbd")
public class LbdController {
  private final LbdService service;
  public LbdController(LbdService service) { this.service = service; }

  @GetMapping("/preview")
  public LbdRecipient preview(@RequestParam(defaultValue = "") String file) throws IOException {
    return service.preview(file);
  }
}
