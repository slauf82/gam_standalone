package de.kopfzentrum.gam.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PasskeyRepository {
  private final JdbcTemplate jdbc;

  public PasskeyRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    ensureSchema();
  }

  public void save(int accountId, String credentialId, String publicKey, String deviceName) {
    ensureSchema();
    if (!tableExists("account_passkeys")) {
      throw new IllegalStateException("Passkey-Tabelle account_passkeys ist nicht verfügbar");
    }
    jdbc.update("""
      INSERT INTO account_passkeys (account_id, credential_id, public_key, device_name, active)
      VALUES (?, ?, ?, ?, TRUE)
      ON DUPLICATE KEY UPDATE
        public_key = VALUES(public_key),
        device_name = VALUES(device_name),
        active = TRUE
      """, accountId, credentialId, publicKey, deviceName);
  }

  public boolean existsActiveForUsername(String username, String credentialId) {
    ensureSchema();
    if (!tableExists("account_passkeys")) {
      return false;
    }
    Integer count = jdbc.queryForObject("""
      SELECT COUNT(*)
      FROM account_passkeys p
      JOIN accounts a ON a.id = p.account_id
      WHERE a.username = ? AND p.credential_id = ? AND p.active = TRUE
      """, Integer.class, username, credentialId);
    return count != null && count > 0;
  }

  public List<String> findCredentialIdsByUsername(String username) {
    ensureSchema();
    if (!tableExists("account_passkeys")) {
      return List.of();
    }
    return jdbc.query("""
      SELECT p.credential_id
      FROM account_passkeys p
      JOIN accounts a ON a.id = p.account_id
      WHERE a.username = ? AND p.active = TRUE
      ORDER BY p.created_at DESC
      """, (rs, row) -> rs.getString("credential_id"), username);
  }

  public int countActiveByUsername(String username) {
    ensureSchema();
    if (!tableExists("account_passkeys")) {
      return 0;
    }
    Integer count = jdbc.queryForObject("""
      SELECT COUNT(*)
      FROM account_passkeys p
      JOIN accounts a ON a.id = p.account_id
      WHERE a.username = ? AND p.active = TRUE
      """, Integer.class, username);
    return count == null ? 0 : count;
  }

  /**
   * Schritt 39n / GitHub v2.0.2:
   * Stellt sicher, dass der lokale Passkey-Testworkflow auch in migrierten
   * Bestandsdatenbanken funktioniert. Vor 39n konnte der Login-Options-Endpunkt
   * mit SQLSyntaxErrorException abbrechen, wenn account_passkeys noch fehlte.
   */
  private synchronized void ensureSchema() {
    try {
      jdbc.execute("""
        CREATE TABLE IF NOT EXISTS account_passkeys (
          id BIGINT NOT NULL AUTO_INCREMENT,
          account_id INT NOT NULL,
          credential_id VARCHAR(512) NOT NULL,
          public_key LONGTEXT NULL,
          device_name VARCHAR(255) NULL,
          sign_count BIGINT NOT NULL DEFAULT 0,
          active BOOLEAN NOT NULL DEFAULT TRUE,
          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
          last_used_at TIMESTAMP NULL,
          PRIMARY KEY (id),
          UNIQUE KEY uk_account_passkeys_credential_id (credential_id),
          KEY idx_account_passkeys_account_id (account_id),
          KEY idx_account_passkeys_active (active)
        )
        """);

      addColumnIfMissing("account_passkeys", "device_name", "VARCHAR(255) NULL");
      addColumnIfMissing("account_passkeys", "sign_count", "BIGINT NOT NULL DEFAULT 0");
      addColumnIfMissing("account_passkeys", "active", "BOOLEAN NOT NULL DEFAULT TRUE");
      addColumnIfMissing("account_passkeys", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
      addColumnIfMissing("account_passkeys", "last_used_at", "TIMESTAMP NULL");
    } catch (Exception ex) {
      // Passkey darf das klassische Login nicht blockieren. Die eigentlichen
      // Passkey-Methoden liefern dann leere Ergebnisse bzw. eine klare Ausnahme
      // beim Speichern, statt das Backend beim Start scheitern zu lassen.
      System.err.println("WARN: Passkey-Schema konnte nicht initialisiert werden: " + ex.getMessage());
    }
  }

  private void addColumnIfMissing(String tableName, String columnName, String definition) {
    if (!tableExists(tableName) || columnExists(tableName, columnName)) {
      return;
    }
    jdbc.execute("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition);
  }

  private boolean tableExists(String tableName) {
    Integer count = jdbc.queryForObject("""
      SELECT COUNT(*)
      FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND LOWER(table_name) = LOWER(?)
      """, Integer.class, tableName);
    return count != null && count > 0;
  }

  private boolean columnExists(String tableName, String columnName) {
    Integer count = jdbc.queryForObject("""
      SELECT COUNT(*)
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND LOWER(table_name) = LOWER(?)
        AND LOWER(column_name) = LOWER(?)
      """, Integer.class, tableName, columnName);
    return count != null && count > 0;
  }
}
