package de.kopfzentrum.gam.system;

import de.kopfzentrum.gam.auth.AccountRepository;
import de.kopfzentrum.gam.invoice.lbd.LbdService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
  private final JdbcTemplate jdbc;
  private final AccountRepository accounts;
  private final LbdService lbdService;

  public SystemStatusController(JdbcTemplate jdbc, AccountRepository accounts, LbdService lbdService) {
    this.jdbc = jdbc;
    this.accounts = accounts;
    this.lbdService = lbdService;
  }

  @GetMapping("/status")
  public SystemStatus status() {
    boolean db = false;
    long accountCount = 0;
    try {
      jdbc.queryForObject("SELECT 1", Integer.class);
      db = true;
      accountCount = accounts.countAccounts();
    } catch (Exception ignored) {}

    var lbd = lbdService.findFirstLbdFile();
    Map<String, Object> modules = new LinkedHashMap<>();
    modules.put("auth", Map.of("status", "phase-1a", "accountsTable", "accounts"));
    modules.put("invoice", Map.of("status", "foundation", "lbd", lbd.isPresent()));
    modules.put("inventory", Map.of("status", "pending"));
    modules.put("permissions", Map.of("status", "role-from-accounts-compatible"));
    modules.put("tasks", Map.of("status", "read-only-frame", "table", "aufgaben"));
    modules.put("approvals", Map.of("status", "read-only-frame", "table", "freigabe"));
    modules.put("personnel", Map.of("status", "read-only-frame", "table", "personal"));
    modules.put("cashbook", Map.of("status", "read-only-frame", "table", "kassenbuch"));
    modules.put("workplace", Map.of("status", "gds-editable", "table", "arbeitsplatz"));
    modules.put("priceList", Map.of("status", "gds-editable", "table", "preisliste"));
    modules.put("compliance", Map.of("status", "gds-editable", "tables", List.of("kontrolle", "inbetriebnahme", "einweisung")));
    modules.put("reports", Map.of("status", "summary-frame"));

    return new SystemStatus(
      db,
      accountCount,
      lbd.isPresent(),
      lbd.map(path -> path.toAbsolutePath().normalize().toString()).orElse(null),
      lbdService.searchFolders(),
      modules
    );
  }
}
