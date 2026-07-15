package de.kopfzentrum.gam.settings;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ModuleSettingsRepository {
  private static final String PREFIX = "module.enabled.";
  private final JdbcTemplate jdbc;

  public ModuleSettingsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @PostConstruct
  void ensureDefaults() {
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_settings (
        setting_key VARCHAR(120) NOT NULL,
        setting_value TEXT NULL,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        updated_by VARCHAR(255) NULL,
        PRIMARY KEY (setting_key)
      )
      """);
    for (String key : supported().keySet()) insertDefault(key, true);
  }

  public Map<String, Boolean> load() {
    var result = new LinkedHashMap<String, Boolean>();
    for (var e : supported().entrySet()) result.put(e.getKey(), bool(PREFIX + e.getKey(), e.getValue()));
    // Kernmodule dürfen nie ausgeblendet werden.
    result.put("dashboard", true);
    result.put("settings", true);
    result.put("users", true);
    result.put("moduleAdmin", true);
    return result;
  }

  public Map<String, Boolean> save(Map<String, Boolean> values, String username) {
    var supported = supported();
    for (var e : supported.entrySet()) {
      boolean value = values.getOrDefault(e.getKey(), e.getValue());
      if (isProtected(e.getKey())) value = true;
      put(PREFIX + e.getKey(), value, username);
    }
    return load();
  }

  private Map<String, Boolean> supported() {
    var m = new LinkedHashMap<String, Boolean>();
    for (String key : new String[]{"dashboard","settings","invoices","invoiceAdmin","inventory","warehouse","patients","appointments","tasks","approvals","orders","personnel","cashbook","workplace","priceList","compliance","marketing","reports","communication","moduleAdmin","users"}) m.put(key, true);
    return m;
  }

  private boolean isProtected(String key) {
    return key.equals("dashboard") || key.equals("settings") || key.equals("users") || key.equals("moduleAdmin");
  }

  private void insertDefault(String key, boolean value) {
    jdbc.update("INSERT IGNORE INTO gam_settings(setting_key, setting_value, updated_by) VALUES(?, ?, 'system')", PREFIX + key, Boolean.toString(value));
  }

  private void put(String key, boolean value, String username) {
    jdbc.update("""
      INSERT INTO gam_settings(setting_key, setting_value, updated_by)
      VALUES(?, ?, ?)
      ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value), updated_by=VALUES(updated_by), updated_at=CURRENT_TIMESTAMP
      """, key, Boolean.toString(value), username);
  }

  private boolean bool(String key, boolean fallback) {
    var values = jdbc.query("SELECT setting_value FROM gam_settings WHERE setting_key=?",
      (ResultSet rs, int row) -> rs.getString(1), key);
    return values.isEmpty() ? fallback : Boolean.parseBoolean(values.get(0));
  }
}
