
package de.kopfzentrum.gam.reports;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import de.kopfzentrum.gam.auth.GamPermissionService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/gam/reports/invoices")
public class InvoiceReportController {
  private final InvoiceReportService service;
  private final GamPermissionService permissions;

  public InvoiceReportController(InvoiceReportService service, GamPermissionService permissions) {
    this.service = service;
    this.permissions = permissions;
  }

  @GetMapping
  public List<InvoiceReportRow> rows(
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate to,
    @RequestParam(required = false) Integer companyId,
    @RequestParam(defaultValue = "overview") String reportType,
    @AuthenticationPrincipal AuthenticatedUser user
  ) {
    requireReport(user, companyId);
    return service.rows(reportType, from, to, companyId);
  }

  @GetMapping("/summary")
  public InvoiceReportSummary summary(
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate to,
    @RequestParam(required = false) Integer companyId,
    @RequestParam(defaultValue = "overview") String reportType,
    @AuthenticationPrincipal AuthenticatedUser user
  ) {
    requireReport(user, companyId);
    return service.summary(reportType, from, to, companyId);
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate to,
    @RequestParam(required = false) Integer companyId,
    @RequestParam(defaultValue = "xlsx") String format,
    @RequestParam(defaultValue = "overview") String reportType,
    @AuthenticationPrincipal AuthenticatedUser user
  ) {
    requireReport(user, companyId);
    List<InvoiceReportRow> rows = service.rows(reportType, from, to, companyId);
    String f = format == null ? "xlsx" : format.toLowerCase();
    byte[] bytes;
    String filename;
    MediaType mediaType;
    switch (f) {
      case "xls" -> {
        bytes = service.xlsHtml(rows);
        filename = "gam-rechnungsreport.xls";
        mediaType = MediaType.parseMediaType("application/vnd.ms-excel");
      }
      case "csv" -> {
        bytes = service.csv(rows);
        filename = "gam-rechnungsreport.csv";
        mediaType = MediaType.parseMediaType("text/csv;charset=UTF-8");
      }
      case "datev" -> {
        bytes = service.datevCsv(rows);
        filename = "gam-datev-vorbereitung.csv";
        mediaType = MediaType.parseMediaType("text/csv;charset=UTF-8");
      }
      default -> {
        bytes = service.xlsx(rows);
        filename = "gam-rechnungsreport.xlsx";
        mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      }
    }
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
      .contentType(mediaType)
      .body(bytes);
  }

  private void requireReport(AuthenticatedUser user, Integer companyId) {
    if (user == null || !permissions.canReport(user.account(), "Rechnungsprogramm", companyId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Report-Berechtigung fuer das Rechnungsprogramm");
    }
  }
}
