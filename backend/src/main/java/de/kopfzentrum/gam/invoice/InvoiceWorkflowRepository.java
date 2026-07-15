package de.kopfzentrum.gam.invoice;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class InvoiceWorkflowRepository {
  private static final List<String> ALL_STATES = List.of("ENTWURF", "PRUEFUNG", "FREIGEGEBEN", "VERSENDET", "ABGESCHLOSSEN");

  private final JdbcTemplate jdbc;
  private final InvoiceWorkflowSettingsRepository settingsRepository;

  public InvoiceWorkflowRepository(JdbcTemplate jdbc, InvoiceWorkflowSettingsRepository settingsRepository) {
    this.jdbc = jdbc;
    this.settingsRepository = settingsRepository;
  }

  @PostConstruct
  void ensureSchema() {
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS invoice_workflow_state (
        invoice_number VARCHAR(80) NOT NULL,
        company_id INT NULL,
        status VARCHAR(32) NOT NULL DEFAULT 'ENTWURF',
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        updated_by VARCHAR(255) NULL,
        PRIMARY KEY (invoice_number, company_id)
      )
      """);
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS invoice_workflow_history (
        id BIGINT NOT NULL AUTO_INCREMENT,
        invoice_number VARCHAR(80) NOT NULL,
        company_id INT NULL,
        from_status VARCHAR(32) NULL,
        to_status VARCHAR(32) NOT NULL,
        note TEXT NULL,
        changed_by VARCHAR(255) NULL,
        changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id),
        INDEX idx_invoice_workflow_history_invoice (invoice_number, company_id, changed_at)
      )
      """);
  }

  @Transactional
  public InvoiceWorkflowState getOrCreate(String invoiceNumber, Integer companyId, String username) {
    Integer count = jdbc.queryForObject(
      "SELECT COUNT(*) FROM invoice_workflow_state WHERE invoice_number=? AND company_id <=> ?",
      Integer.class, invoiceNumber, companyId);
    if (count == null || count == 0) {
      jdbc.update("INSERT INTO invoice_workflow_state(invoice_number, company_id, status, updated_by) VALUES(?, ?, 'ENTWURF', ?)", invoiceNumber, companyId, username);
      jdbc.update("INSERT INTO invoice_workflow_history(invoice_number, company_id, from_status, to_status, note, changed_by) VALUES(?, ?, NULL, 'ENTWURF', ?, ?)", invoiceNumber, companyId, "Workflow automatisch initialisiert", username);
    }
    return load(invoiceNumber, companyId);
  }

  @Transactional
  public InvoiceWorkflowState transition(String invoiceNumber, Integer companyId, String requestedStatus, String note, String username) {
    InvoiceWorkflowState current = getOrCreate(invoiceNumber, companyId, username);
    String target = normalize(requestedStatus);
    if (!current.allowedTransitions().contains(target)) {
      throw new IllegalArgumentException("Ungueltiger Workflow-Uebergang: " + current.status() + " -> " + target);
    }
    jdbc.update("UPDATE invoice_workflow_state SET status=?, updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE invoice_number=? AND company_id <=> ?",
      target, username, invoiceNumber, companyId);
    jdbc.update("INSERT INTO invoice_workflow_history(invoice_number, company_id, from_status, to_status, note, changed_by) VALUES(?, ?, ?, ?, ?, ?)",
      invoiceNumber, companyId, current.status(), target, note, username);
    return load(invoiceNumber, companyId);
  }

  private InvoiceWorkflowState load(String invoiceNumber, Integer companyId) {
    var state = jdbc.queryForMap("SELECT status, updated_at, updated_by FROM invoice_workflow_state WHERE invoice_number=? AND company_id <=> ?", invoiceNumber, companyId);
    String status = normalize(String.valueOf(state.get("status")));
    Timestamp ts = (Timestamp) state.get("updated_at");
    List<InvoiceWorkflowEntry> history = jdbc.query("""
      SELECT id, invoice_number, company_id, from_status, to_status, note, changed_by, changed_at
      FROM invoice_workflow_history
      WHERE invoice_number=? AND company_id <=> ?
      ORDER BY changed_at DESC, id DESC
      """, (rs, row) -> new InvoiceWorkflowEntry(
        rs.getLong("id"), rs.getString("invoice_number"), (Integer) rs.getObject("company_id"),
        rs.getString("from_status"), rs.getString("to_status"), rs.getString("note"),
        rs.getString("changed_by"), rs.getTimestamp("changed_at").toLocalDateTime()), invoiceNumber, companyId);
    InvoiceWorkflowSettings settings = settingsRepository.load();
    List<String> steps = configuredSteps(settings);
    List<String> transitions = allowedTransitions(status, settings);
    return new InvoiceWorkflowState(invoiceNumber, companyId, status,
      ts == null ? LocalDateTime.now() : ts.toLocalDateTime(), String.valueOf(state.get("updated_by")),
      transitions, steps, settings, history);
  }

  private List<String> configuredSteps(InvoiceWorkflowSettings settings) {
    java.util.ArrayList<String> steps = new java.util.ArrayList<>();
    steps.add("ENTWURF");
    if (settings.reviewEnabled()) steps.add("PRUEFUNG");
    if (settings.approvalEnabled()) steps.add("FREIGEGEBEN");
    if (settings.shippingEnabled()) steps.add("VERSENDET");
    steps.add("ABGESCHLOSSEN");
    return List.copyOf(steps);
  }

  private List<String> allowedTransitions(String status, InvoiceWorkflowSettings settings) {
    // Bereits laufende Workflows bleiben bedienbar, auch wenn ein Schritt nachträglich deaktiviert wurde.
    if ("PRUEFUNG".equals(status)) return settings.approvalEnabled() ? List.of("ENTWURF", "FREIGEGEBEN") : List.of("ENTWURF", "ABGESCHLOSSEN");
    if ("FREIGEGEBEN".equals(status)) return settings.shippingEnabled() ? List.of("VERSENDET") : List.of("ABGESCHLOSSEN");
    if ("VERSENDET".equals(status)) return List.of("ABGESCHLOSSEN");
    if ("ABGESCHLOSSEN".equals(status)) return List.of();
    if ("ENTWURF".equals(status)) {
      if (settings.reviewEnabled()) return List.of("PRUEFUNG");
      if (settings.approvalEnabled()) return List.of("FREIGEGEBEN");
      if (settings.shippingEnabled()) return List.of("VERSENDET");
      return List.of("ABGESCHLOSSEN");
    }
    return List.of();
  }

  private String normalize(String status) {
    if (status == null || status.isBlank()) throw new IllegalArgumentException("Workflow-Status fehlt.");
    String normalized = status.trim().toUpperCase(Locale.ROOT).replace('Ü', 'U');
    if (!ALL_STATES.contains(normalized)) throw new IllegalArgumentException("Unbekannter Workflow-Status: " + status);
    return normalized;
  }
}
