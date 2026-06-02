package de.kopfzentrum.gam.system;

import de.kopfzentrum.gam.invoice.lbd.LbdService;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class StartupCheckController {
  private final JdbcTemplate jdbc;
  private final LbdService lbdService;
  private final Environment environment;

  @Value("${spring.datasource.url:}")
  private String datasourceUrl;

  @Value("${zugferd.enabled:true}")
  private boolean zugferdEnabled;

  @Value("${app.legacy-login.require-totp-when-secret-exists:false}")
  private boolean requireTotpWhenSecretExists;

  public StartupCheckController(JdbcTemplate jdbc, LbdService lbdService, Environment environment) {
    this.jdbc = jdbc;
    this.lbdService = lbdService;
    this.environment = environment;
  }

  @GetMapping("/startup-check")
  public Map<String, Object> startupCheck() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("timestamp", OffsetDateTime.now().toString());
    result.put("application", "GAM 2.0 Standalone");
    String[] profiles = environment.getActiveProfiles();
    result.put("profile", profiles.length == 0 ? "default" : String.join(",", profiles));
    result.put("datasourceUrl", maskDatasource(datasourceUrl));
    result.put("zugferdEnabled", zugferdEnabled);
    result.put("requireTotpWhenSecretExists", requireTotpWhenSecretExists);

    try {
      Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
      result.put("database", Map.of("ok", one != null && one == 1));
    } catch (Exception ex) {
      result.put("database", Map.of("ok", false, "error", ex.getClass().getSimpleName(), "message", ex.getMessage()));
    }

    var lbd = lbdService.findFirstLbdFile();
    result.put("lbd", Map.of(
        "available", lbd.isPresent(),
        "file", lbd.map(path -> path.toAbsolutePath().normalize().toString()).orElse(""),
        "searchFolders", lbdService.searchFolders()
    ));

    result.put("nextChecks", new String[] {
        "GET /api/system/status",
        "POST /api/auth/login",
        "GET /api/invoices/lbd/preview",
        "GET /api/invoices",
        "GET /api/inventory/devices",
        "GET /api/warehouse/items"
    });
    return result;
  }

  private String maskDatasource(String value) {
    if (value == null || value.isBlank()) return "";
    return value.replaceAll("(?i)(password=)[^&;]+", "$1***");
  }
}
