package de.kopfzentrum.gam.invoice;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceWorkflowSettingsRepository {
  private static final String KEY = "invoice.workflow";
  private final JdbcTemplate jdbc;

  public InvoiceWorkflowSettingsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @PostConstruct
  void ensureSchema() {
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_settings (
        setting_key VARCHAR(120) NOT NULL,
        setting_value TEXT NULL,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        updated_by VARCHAR(255) NULL,
        PRIMARY KEY (setting_key)
      )
      """);
    insertDefault("invoice.workflow.review.enabled", "true");
    insertDefault("invoice.workflow.approval.enabled", "true");
    insertDefault("invoice.workflow.shipping.enabled", "true");
    insertDefault("invoice.workflow.autoCompleteAfterShipping", "false");
  }

  private void insertDefault(String key, String value) {
    jdbc.update("INSERT IGNORE INTO gam_settings(setting_key, setting_value, updated_by) VALUES(?, ?, 'system')", key, value);
  }

  public InvoiceWorkflowSettings load() {
    return new InvoiceWorkflowSettings(
      bool("invoice.workflow.review.enabled", true),
      bool("invoice.workflow.approval.enabled", true),
      bool("invoice.workflow.shipping.enabled", true),
      bool("invoice.workflow.autoCompleteAfterShipping", false)
    );
  }

  public InvoiceWorkflowSettings save(InvoiceWorkflowSettings value, String username) {
    put("invoice.workflow.review.enabled", value.reviewEnabled(), username);
    put("invoice.workflow.approval.enabled", value.approvalEnabled(), username);
    put("invoice.workflow.shipping.enabled", value.shippingEnabled(), username);
    put("invoice.workflow.autoCompleteAfterShipping", value.autoCompleteAfterShipping(), username);
    return load();
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
